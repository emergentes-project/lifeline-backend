package com.upc.pe.lifeline.iam.application.internal.commandservices;

import com.upc.pe.lifeline.iam.application.ports.PasswordHasher;
import com.upc.pe.lifeline.iam.domain.exceptions.EmailAlreadyExistsException;
import com.upc.pe.lifeline.iam.domain.exceptions.IamConflictException;
import com.upc.pe.lifeline.iam.domain.exceptions.ProfessionalLicenseAlreadyExistsException;
import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.SignInUserCommand;
import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalRegisteredEvent;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesName;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.ProfessionalCategory;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.CitizenProfileRepository;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserCommandServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CitizenProfileRepository citizens = mock(CitizenProfileRepository.class);
    private final MedicalProfileRepository medical = mock(MedicalProfileRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final UserCommandServiceImpl service = new UserCommandServiceImpl(
            users, citizens, medical, roles, passwords, events);

    @Test
    void registrationStoresBothProfilesAndPublishesOnlyTheMedicalEvent() {
        when(roles.findByCode(RolesCode.CIT1)).thenReturn(Optional.of(new Role(RolesCode.CIT1, RolesName.CITIZEN)));
        when(roles.findByCode(RolesCode.MED2))
                .thenReturn(Optional.of(new Role(RolesCode.MED2, RolesName.MEDICAL_PROFESSIONAL)));
        when(passwords.hash("secret")).thenReturn("encoded-secret");

        User citizen = service.handle(new RegisterCitizenCommand("Citizen@Example.com", "secret", "Ana",
                "Perez", null, "Lima", "Miraflores", "Av. 1", LocalDate.of(1995, 1, 1), null, null));
        User professional = service.handle(new RegisterMedicalProfessionalCommand("Doctor@Example.com", "secret",
                "Luis", "Ruiz", null, "DOCTOR", "CMP-123", null));

        assertThat(citizen.getEmail()).isEqualTo("citizen@example.com");
        assertThat(professional.getPassword()).isEqualTo("encoded-secret");
        ArgumentCaptor<CitizenProfile> citizenProfile = ArgumentCaptor.forClass(CitizenProfile.class);
        ArgumentCaptor<MedicalProfile> medicalProfile = ArgumentCaptor.forClass(MedicalProfile.class);
        verify(citizens).saveAndFlush(citizenProfile.capture());
        verify(medical).saveAndFlush(medicalProfile.capture());
        assertThat(citizenProfile.getValue().getCity()).isEqualTo("Lima");
        assertThat(medicalProfile.getValue().getProfessionalCategory()).isEqualTo(ProfessionalCategory.DOCTOR);
        verify(events).publishEvent(new MedicalProfessionalRegisteredEvent(professional.getId()));

        when(users.existsByEmail("citizen@example.com")).thenReturn(true);
        when(medical.existsByProfessionalLicenseNumber("CMP-123")).thenReturn(true);
        assertThatThrownBy(() -> service.handle(new RegisterCitizenCommand("Citizen@Example.com", "secret", "Ana",
                "Perez", null, "Lima", "Miraflores", "Av. 1", LocalDate.of(1995, 1, 1), null, null)))
                .isInstanceOf(EmailAlreadyExistsException.class);
        assertThatThrownBy(() -> service.handle(new RegisterMedicalProfessionalCommand("other@example.com", "secret",
                "Luis", "Ruiz", null, "DOCTOR", "CMP-123", null)))
                .isInstanceOf(ProfessionalLicenseAlreadyExistsException.class);

        when(users.existsByEmail("citizen@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.handle(new RegisterCitizenCommand("Citizen@Example.com", "secret", "Ana",
                "Perez", null, "Lima", "Miraflores", "Av. 1", LocalDate.of(1995, 1, 1), null, null)))
                .isInstanceOf(IamConflictException.class);
    }

    @Test
    void signInRequiresMatchingPasswordAndNormalizesEmail() {
        User user = new User("citizen@example.com", "encoded-secret", "Ana", "Perez", null,
                new Role(RolesCode.CIT1, RolesName.CITIZEN));
        when(users.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("secret", "encoded-secret")).thenReturn(true);

        assertThat(service.handle(new SignInUserCommand("Citizen@Example.com", "secret"))).isSameAs(user);
        assertThatThrownBy(() -> service.handle(new SignInUserCommand("Citizen@Example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
