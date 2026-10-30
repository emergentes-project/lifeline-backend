package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.iam.domain.model.entities.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {
    @Modifying
    @Query("delete from RevokedAccessToken token where token.expiresAt < :instant")
    void deleteExpired(Instant instant);
}
