package com.upc.pe.lifeline.shared.infrastructure.security;

import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfigurationTest.MvcConfiguration.class, SecurityConfiguration.class,
        JwtService.class, RestAuthenticationEntryPoint.class, GlobalExceptionHandler.class,
        SecurityConfigurationTest.ProbeController.class})
@TestPropertySource(properties = {
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "authorization.jwt.expiration=PT1H",
        "authorization.cors.allowed-origins=http://localhost:4200"
})
class SecurityConfigurationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void guestCanSubmitAnEmergencyAndUploadItsPhoto() throws Exception {
        mvc.perform(post("/api/v1/consultation-submissions")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/consultation-submissions/" + UUID.randomUUID() + "/photo"))
                .andExpect(status().isOk());
    }

    @Test
    void guestCannotReadPrivateSubmissions() throws Exception {
        mvc.perform(get("/api/v1/consultation-submissions"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void internalOperationsRequireAServiceToken() throws Exception {
        String userToken = jwtService.generateToken(new UserId(UUID.randomUUID()), "MEDICAL_PROFESSIONAL");
        String serviceToken = jwtService.generateServiceToken("lifeline-worker");

        mvc.perform(get("/api/v1/internal/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/internal/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    static class MvcConfiguration {
        @Bean
        static ConversionService conversionService() {
            return ApplicationConversionService.getSharedInstance();
        }

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }
    }

    @RestController
    public static class ProbeController {
        @GetMapping({"/api/v1/internal/probe", "/api/v1/consultation-submissions"})
        public Map<String, String> read() {
            return Map.of("message", "ok");
        }

        @PostMapping({"/api/v1/consultation-submissions", "/api/v1/consultation-submissions/{id}/photo"})
        public Map<String, String> submit() {
            return Map.of("message", "received");
        }
    }
}
