package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class OutboxClaimStore {
    private final EntityManager entityManager;
    private final OutboxEventRepository events;

    public OutboxClaimStore(EntityManager entityManager, OutboxEventRepository events) {
        this.entityManager = entityManager;
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int releaseExpired(LocalDateTime staleBefore, LocalDateTime now) {
        int released = entityManager.createNativeQuery("""
                update outbox_events
                set status = 'PENDING', updated_at = :now
                where status = 'PUBLISHING'
                  and updated_at < :staleBefore
                """)
                .setParameter("now", now)
                .setParameter("staleBefore", staleBefore)
                .executeUpdate();
        entityManager.clear();
        return released;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claim(LocalDateTime now, int batchSize) {
        List<UUID> claimed = postgres() ? claimPostgres(now, batchSize) : claimWithoutSkipLocked(now, batchSize);
        entityManager.clear();
        return claimed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID id, LocalDateTime publishedAt) {
        events.findById(id).ifPresent(event -> {
            event.setStatus("PUBLISHED");
            event.setPublishedAt(publishedAt);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID id, LocalDateTime now, Duration retryDelay, int maxAttempts) {
        events.findById(id).ifPresent(event -> {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            if (attempts >= maxAttempts) {
                event.setStatus("FAILED");
                event.setNextAttemptAt(null);
            } else {
                event.setStatus("PENDING");
                event.setNextAttemptAt(now.plus(retryDelay.multipliedBy(attempts)));
            }
        });
    }

    private boolean postgres() {
        return Boolean.TRUE.equals(entityManager.unwrap(Session.class).doReturningWork(connection ->
                connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")));
    }

    private List<UUID> claimPostgres(LocalDateTime now, int batchSize) {
        String sql = """
                with due as (
                    select id
                    from outbox_events
                    where status = 'PENDING'
                      and (next_attempt_at is null or next_attempt_at <= :now)
                    order by created_at
                    for update skip locked
                    limit %d
                )
                update outbox_events as event
                set status = 'PUBLISHING', updated_at = :now
                from due
                where event.id = due.id
                returning event.id
                """.formatted(batchSize);
        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("now", now)
                .getResultList();
        return uuids(rows);
    }

    private List<UUID> claimWithoutSkipLocked(LocalDateTime now, int batchSize) {
        List<?> rows = entityManager.createNativeQuery("""
                select id
                from outbox_events
                where status = 'PENDING'
                  and (next_attempt_at is null or next_attempt_at <= :now)
                order by created_at
                limit %d
                """.formatted(batchSize))
                .setParameter("now", now)
                .getResultList();
        List<UUID> ids = uuids(rows);
        for (UUID id : ids) {
            entityManager.createNativeQuery("""
                    update outbox_events
                    set status = 'PUBLISHING', updated_at = :now
                    where id = :id
                    """)
                    .setParameter("now", now)
                    .setParameter("id", id)
                    .executeUpdate();
        }
        return ids;
    }

    private List<UUID> uuids(List<?> rows) {
        List<UUID> ids = new ArrayList<>(rows.size());
        for (Object row : rows) {
            ids.add(uuid(row));
        }
        return ids;
    }

    private UUID uuid(Object value) {
        if (value instanceof Object[] row) {
            return uuid(row[0]);
        }
        if (value instanceof UUID id) {
            return id;
        }
        if (value instanceof byte[] bytes && bytes.length == 16) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }
}
