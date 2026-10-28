package com.upc.pe.lifeline.iam.interfaces.rest;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iam_read_logout;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
@ActiveProfiles("iam-read-logout-test")
class IamReadAndSignOutIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void rolesArePublicAndCurrentUserRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CIT1"))
                .andExpect(jsonPath("$[1].code").value("MED2"));

        User citizen = registerCitizen();
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(citizen.getId().toString()))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    void signOutRevokesOnlyTheCurrentToken() throws Exception {
        User citizen = registerCitizen();
        String firstToken = bearer(citizen);
        String secondToken = bearer(citizen);
        assertThat(firstToken).isNotEqualTo(secondToken);

        mvc.perform(post("/api/v1/authentication/sign-out")
                        .header(HttpHeaders.AUTHORIZATION, firstToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, firstToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, secondToken))
                .andExpect(status().isOk());
    }

    private User registerCitizen() {
        return userCommandService.handle(new RegisterCitizenCommand(UUID.randomUUID() + "@example.com",
                "secret", "Ana", "Perez", null, "Lima", "Lince", "Av. 1",
                LocalDate.of(1995, 1, 1), null, null));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(new UserId(user.getId()), "CITIZEN");
    }
}
