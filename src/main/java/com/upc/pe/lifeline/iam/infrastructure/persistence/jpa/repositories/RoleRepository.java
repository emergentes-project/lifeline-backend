package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByCode(RolesCode code);
}
