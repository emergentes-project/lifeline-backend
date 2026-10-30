package com.upc.pe.lifeline.iam.application.internal.queryservices;

import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.queries.GetAllRolesQuery;
import com.upc.pe.lifeline.iam.domain.services.RoleQueryService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleQueryServiceImpl implements RoleQueryService {
    private final RoleRepository roleRepository;

    public RoleQueryServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> handle(GetAllRolesQuery query) {
        return roleRepository.findAllByOrderByCodeAsc();
    }
}
