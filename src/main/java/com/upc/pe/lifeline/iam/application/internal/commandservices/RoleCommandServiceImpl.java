package com.upc.pe.lifeline.iam.application.internal.commandservices;

import com.upc.pe.lifeline.iam.domain.model.commands.SeedRolesCommand;
import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesName;
import com.upc.pe.lifeline.iam.domain.services.RoleCommandService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RoleCommandServiceImpl implements RoleCommandService {
    private final RoleRepository roleRepository;

    public RoleCommandServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void handle(SeedRolesCommand command) {
        seed(RolesCode.CIT1, RolesName.CITIZEN);
        seed(RolesCode.MED2, RolesName.MEDICAL_PROFESSIONAL);
    }

    private void seed(RolesCode code, RolesName name) {
        if (roleRepository.existsByCode(code)) {
            return;
        }
        try {
            roleRepository.saveAndFlush(new Role(code, name));
        } catch (DataIntegrityViolationException exception) {
            if (!roleRepository.existsByCode(code)) {
                throw exception;
            }
        }
    }
}
