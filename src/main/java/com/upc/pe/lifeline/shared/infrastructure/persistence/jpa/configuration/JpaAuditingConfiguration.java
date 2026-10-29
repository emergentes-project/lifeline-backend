package com.upc.pe.lifeline.shared.infrastructure.persistence.jpa.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfiguration {
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return new UtcDateTimeProvider();
    }

    private static final class UtcDateTimeProvider implements DateTimeProvider {
        @Override
        public Optional<TemporalAccessor> getNow() {
            return Optional.of(LocalDateTime.now(ZoneOffset.UTC));
        }
    }
}
