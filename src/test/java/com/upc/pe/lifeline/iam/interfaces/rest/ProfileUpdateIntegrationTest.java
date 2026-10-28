package com.upc.pe.lifeline.iam.interfaces.rest;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile_updates;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
@ActiveProfiles("profile-update-test")
class ProfileUpdateIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private MedicalProfileRepository medicalProfileRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void citizenCanUpdateOwnAccountAndProfile() throws Exception {
        User citizen = userCommandService.handle(new RegisterCitizenCommand(UUID.randomUUID() + "@example.com",
                "secret", "Ana", "Perez", null, "Lima", "Lince", "Av. 1",
                LocalDate.of(1995, 1, 1), null, null));
        String token = token(citizen, "CITIZEN");

        mvc.perform(put("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"firstName":"Ana María","lastName":"Pérez","phone":"999111222"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana María"));

        mvc.perform(put("/api/v1/citizen-profiles/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city":"Cusco","district":"Centro","address":"Av. 2",
                                 "emergencyContactName":"Luis","emergencyContactPhone":"988777666",
                                 "preExistingConditions":"Asma","allergies":"Penicilina"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Cusco"))
                .andExpect(jsonPath("$.emergencyContactName").value("Luis"))
                .andExpect(jsonPath("$.dateOfBirth").value("1995-01-01"));

        mvc.perform(get("/api/v1/citizen-profiles/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies").value("Penicilina"));
        mvc.perform(put("/api/v1/medical-profiles/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changedMedicalLicenseIsReverifiedAndUnchangedLicenseKeepsItsStatus() throws Exception {
        userCommandService.handle(new RegisterMedicalProfessionalCommand(UUID.randomUUID() + "@example.com",
                "secret", "First", "Doctor", null, "DOCTOR", "A3480045365123", null));
        User professional = userCommandService.handle(new RegisterMedicalProfessionalCommand(
                UUID.randomUUID() + "@example.com", "secret", "Second", "Doctor", null,
                "DOCTOR", "B3480045365123", null));
        String token = token(professional, "MEDICAL_PROFESSIONAL");
        var original = medicalProfileRepository.findByUser_Id(professional.getId()).orElseThrow();
        assertThat(original.getVerificationStatus()).isEqualTo("APPROVED");
        var verifiedAt = original.getVerifiedAt();

        mvc.perform(put("/api/v1/medical-profiles/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalCategory":"DOCTOR","professionalLicenseNumber":"b3480045365123",
                                 "specialty":"Cardiología"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"));
        assertThat(medicalProfileRepository.findByUser_Id(professional.getId()).orElseThrow().getVerifiedAt())
                .isEqualTo(verifiedAt);

        mvc.perform(put("/api/v1/medical-profiles/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalCategory":"DOCTOR","professionalLicenseNumber":"A3480045365123",
                                 "specialty":"Cardiología"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"));

        mvc.perform(put("/api/v1/medical-profiles/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalCategory":"DOCTOR","professionalLicenseNumber":"C3480045365123",
                                 "specialty":"Cardiología"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"));
        mvc.perform(get("/api/v1/medical-profiles/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalLicenseNumber").value("C3480045365123"));
    }

    private String token(User user, String role) {
        return "Bearer " + jwtService.generateToken(new UserId(user.getId()), role);
    }
}
