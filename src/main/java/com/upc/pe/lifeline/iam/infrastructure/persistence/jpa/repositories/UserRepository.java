package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(UUID id);
}
