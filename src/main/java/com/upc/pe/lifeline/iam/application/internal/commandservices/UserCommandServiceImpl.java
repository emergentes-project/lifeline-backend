package com.upc.pe.lifeline.iam.application.internal.commandservices;

import com.upc.pe.lifeline.iam.application.ports.PasswordHasher;
import com.upc.pe.lifeline.iam.domain.exceptions.IamConflictException;
import com.upc.pe.lifeline.iam.domain.exceptions.EmailAlreadyExistsException;
import com.upc.pe.lifeline.iam.domain.exceptions.RoleNotFoundException;
import com.upc.pe.lifeline.iam.domain.exceptions.UserNotCreatedException;
import com.upc.pe.lifeline.iam.domain.exceptions.ProfileNotUpdatedException;
import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.SignInUserCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.SignOutUserCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateUserCommand;
import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalRegisteredEvent;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.CitizenProfileRepository;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.upc.pe.lifeline.shared.application.ports.AccessTokenRevocationStore;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final MedicalProfileRepository medicalProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessTokenRevocationStore accessTokenRevocationStore;
    private final JwtService jwtService;

    public UserCommandServiceImpl(UserRepository userRepository,
                                  CitizenProfileRepository citizenProfileRepository,
                                  MedicalProfileRepository medicalProfileRepository,
                                  RoleRepository roleRepository, PasswordHasher passwordHasher,
                                  ApplicationEventPublisher eventPublisher,
                                  AccessTokenRevocationStore accessTokenRevocationStore, JwtService jwtService) {
        this.userRepository = userRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.medicalProfileRepository = medicalProfileRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.accessTokenRevocationStore = accessTokenRevocationStore;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public User handle(RegisterCitizenCommand command) {
        String email = normalizeEmail(command.email());
        ensureEmailAvailable(email);
        User user = new User(command, passwordHasher.hash(command.password()), role(RolesCode.CIT1));
        CitizenProfile profile = new CitizenProfile(command, user);
        try {
            userRepository.saveAndFlush(user);
            citizenProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            throw new IamConflictException("Registration conflicts with an existing record", exception);
        } catch (DataAccessException exception) {
            throw new UserNotCreatedException(exception);
        }
        return user;
    }

    @Override
    @Transactional
    public User handle(RegisterMedicalProfessionalCommand command) {
        String email = normalizeEmail(command.email());
        ensureEmailAvailable(email);
        User user = new User(command, passwordHasher.hash(command.password()), role(RolesCode.MED2));
        MedicalProfile profile = new MedicalProfile(command, user);
        try {
            userRepository.saveAndFlush(user);
            medicalProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            throw new IamConflictException("Registration conflicts with an existing record", exception);
        } catch (DataAccessException exception) {
            throw new UserNotCreatedException(exception);
        }
        eventPublisher.publishEvent(new MedicalProfessionalRegisteredEvent(user.getId()));
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User handle(SignInUserCommand command) {
        User user = userRepository.findByEmail(normalizeEmail(command.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordHasher.matches(command.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return user;
    }

    @Override
    @Transactional
    public User handle(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.update(command);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IamConflictException("User update conflicts with an existing record", exception);
        } catch (DataAccessException exception) {
            throw new ProfileNotUpdatedException(exception);
        }
    }

    @Override
    @Transactional
    public void handle(SignOutUserCommand command) {
        var claims = jwtService.parseClaims(command.accessToken());
        if (!"access".equals(claims.get("token_use", String.class))) {
            throw new BadCredentialsException("Invalid access token");
        }
        if (!command.userId().toString().equals(claims.getSubject())) {
            throw new BadCredentialsException("Invalid access token");
        }
        accessTokenRevocationStore.revoke(command.accessToken(), claims.getExpiration().toInstant());
    }

    private Role role(RolesCode code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new RoleNotFoundException(code));
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
