package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
