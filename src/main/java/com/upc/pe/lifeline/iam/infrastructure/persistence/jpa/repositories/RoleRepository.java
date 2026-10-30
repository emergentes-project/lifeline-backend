package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByCode(RolesCode code);
    Optional<Role> findByCode(RolesCode code);
    List<Role> findAllByOrderByCodeAsc();
}
