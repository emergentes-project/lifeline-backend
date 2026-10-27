package com.upc.pe.lifeline.shared;

import com.cloudinary.Cloudinary;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.upc.pe.lifeline.shared.domain.model.entities.AuditableModel;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import com.upc.pe.lifeline.shared.interfaces.rest.GlobalExceptionHandler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shared;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
class SharedConfigurationTest {
    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean(name = "auditingDateTimeProvider")
    private DateTimeProvider dateTimeProvider;

    @Test
    void sharedBeansLoadWithTheRealSpringBootConfiguration() {
        assertThat(imageStorageService).isNotNull();
        assertThat(cloudinary.uploader()).isNotNull();
        assertThat(cloudinary.config.secure).isTrue();
        assertThat(jwtService).isNotNull();
        assertThat(securityFilterChain).isNotNull();
        assertThat(exceptionHandler).isNotNull();
        String password = "test-password";
        String hash = passwordEncoder.encode(password);
        assertThat(hash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
    }

    @Test
    @Transactional
    void aggregatePreservesTheMobileIdAndRegistersDomainEvents() {
        Instant now = Instant.parse("2026-10-27T12:00:00Z");
        when(dateTimeProvider.getNow()).thenReturn(Optional.of(now));
        UUID suppliedId = UUID.randomUUID();
        AggregateProbe aggregate = new AggregateProbe(suppliedId);
        aggregate.addDomainEvent("consultation-received");
        assertThat(aggregate.isNew()).isTrue();

        new SimpleJpaRepository<>(AggregateProbe.class, entityManager).saveAndFlush(aggregate);

        assertThat(aggregate.getId()).isEqualTo(suppliedId);
        assertThat(aggregate.getCreatedAt()).isEqualTo(now);
        assertThat(aggregate.getUpdatedAt()).isEqualTo(now);
        assertThat(aggregate.isNew()).isFalse();
        assertThat(aggregate.getDomainEvents()).containsExactly("consultation-received");
        aggregate.clearEvents();
        assertThat(aggregate.getDomainEvents()).isEmpty();
        entityManager.clear();
        assertThat(entityManager.find(AggregateProbe.class, suppliedId).isNew()).isFalse();
    }

    @Test
    @Transactional
    void entityGeneratesItsIdAndUpdatesOnlyTheModificationDate() {
        Instant created = Instant.parse("2026-10-27T12:00:00Z");
        Instant updated = created.plusSeconds(60);
        when(dateTimeProvider.getNow()).thenReturn(Optional.of(created));
        EntityProbe entity = new EntityProbe();
        var repository = new SimpleJpaRepository<>(EntityProbe.class, entityManager);
        repository.saveAndFlush(entity);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedAt()).isEqualTo(created);
        when(dateTimeProvider.getNow()).thenReturn(Optional.of(updated));
        entity.label = "updated";
        repository.saveAndFlush(entity);
        entityManager.clear();
        EntityProbe reloaded = entityManager.find(EntityProbe.class, entity.getId());

        assertThat(reloaded.getCreatedAt()).isEqualTo(created);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updated);
        assertThat(reloaded.isNew()).isFalse();
    }

    @Entity
    @Table(name = "aggregate_probes")
    public static class AggregateProbe extends AuditableAbstractAggregateRoot<AggregateProbe> {
        protected AggregateProbe() {
        }

        AggregateProbe(UUID id) {
            super(id);
        }
    }

    @Entity
    @Table(name = "entity_probes")
    public static class EntityProbe extends AuditableModel {
        @Column(nullable = false)
        private String label = "initial";

        protected EntityProbe() {
        }
    }
}
