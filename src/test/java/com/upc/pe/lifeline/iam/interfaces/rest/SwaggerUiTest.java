package com.upc.pe.lifeline.iam.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("swagger-test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:swagger_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
class SwaggerUiTest {
    @Autowired
    private WebApplicationContext context;

    @Test
    void exposesAuthenticationOperationsAndSwaggerUiWithoutACloudinaryAccount() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var apiDocs = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(apiDocs).contains("/api/v1/authentication/sign-up/citizens",
                "/api/v1/authentication/sign-up/medical-professionals",
                "/api/v1/authentication/sign-in", "bearerAuth");

        var swaggerStatus = mvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertThat(swaggerStatus).isBetween(200, 399);
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
