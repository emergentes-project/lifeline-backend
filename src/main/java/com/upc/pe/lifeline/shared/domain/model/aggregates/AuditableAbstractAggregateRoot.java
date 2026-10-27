package com.upc.pe.lifeline.shared.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableAbstractAggregateRoot<T extends AuditableAbstractAggregateRoot<T>>
        extends AbstractAggregateRoot<T> implements Persistable<UUID> {
    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    protected AuditableAbstractAggregateRoot() {
        this(UUID.randomUUID());
    }

    protected AuditableAbstractAggregateRoot(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    protected void markNotNew() {
        isNew = false;
    }

    public void addDomainEvent(Object event) {
        super.registerEvent(event);
    }

    public List<Object> getDomainEvents() {
        return List.copyOf(super.domainEvents());
    }

    public void clearEvents() {
        super.clearDomainEvents();
    }
}
