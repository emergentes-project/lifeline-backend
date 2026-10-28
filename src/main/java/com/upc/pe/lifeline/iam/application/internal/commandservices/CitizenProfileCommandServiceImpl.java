package com.upc.pe.lifeline.iam.application.internal.commandservices;

import com.upc.pe.lifeline.iam.domain.exceptions.IamConflictException;
import com.upc.pe.lifeline.iam.domain.exceptions.ProfileNotUpdatedException;
import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateCitizenProfileCommand;
import com.upc.pe.lifeline.iam.domain.services.CitizenProfileCommandService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.CitizenProfileRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CitizenProfileCommandServiceImpl implements CitizenProfileCommandService {
    private final CitizenProfileRepository citizenProfileRepository;

    public CitizenProfileCommandServiceImpl(CitizenProfileRepository citizenProfileRepository) {
        this.citizenProfileRepository = citizenProfileRepository;
    }

    @Override
    @Transactional
    public CitizenProfile handle(UpdateCitizenProfileCommand command) {
        CitizenProfile profile = citizenProfileRepository.findByUser_Id(command.userId())
                .orElseThrow(() -> new NoSuchElementException("Citizen profile not found"));
        profile.update(command);
        try {
            return citizenProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            throw new IamConflictException("Citizen profile update conflicts with an existing record", exception);
        } catch (DataAccessException exception) {
            throw new ProfileNotUpdatedException(exception);
        }
    }
}
