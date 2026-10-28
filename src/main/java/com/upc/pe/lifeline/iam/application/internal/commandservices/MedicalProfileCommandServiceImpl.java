package com.upc.pe.lifeline.iam.application.internal.commandservices;

import com.upc.pe.lifeline.iam.domain.exceptions.IamConflictException;
import com.upc.pe.lifeline.iam.domain.exceptions.ProfileNotUpdatedException;
import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateMedicalProfileCommand;
import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalLicenseUpdatedEvent;
import com.upc.pe.lifeline.iam.domain.services.MedicalProfileCommandService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class MedicalProfileCommandServiceImpl implements MedicalProfileCommandService {
    private final MedicalProfileRepository medicalProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MedicalProfileCommandServiceImpl(MedicalProfileRepository medicalProfileRepository,
                                            ApplicationEventPublisher eventPublisher) {
        this.medicalProfileRepository = medicalProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MedicalProfile handle(UpdateMedicalProfileCommand command) {
        MedicalProfile profile = medicalProfileRepository.findByUser_Id(command.userId())
                .orElseThrow(() -> new NoSuchElementException("Medical profile not found"));
        boolean licenseChanged = profile.update(command);
        try {
            medicalProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            throw new IamConflictException("Medical profile update conflicts with an existing record", exception);
        } catch (DataAccessException exception) {
            throw new ProfileNotUpdatedException(exception);
        }
        if (licenseChanged) {
            eventPublisher.publishEvent(new MedicalProfessionalLicenseUpdatedEvent(command.userId()));
        }
        return profile;
    }
}
