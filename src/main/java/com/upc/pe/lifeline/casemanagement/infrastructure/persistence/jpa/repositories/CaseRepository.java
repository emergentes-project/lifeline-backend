package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.PreliminaryPriority;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<Case, UUID> {
    Optional<Case> findByConsultationSubmission_Id(UUID consultationId);

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    Optional<Case> findById(UUID id);

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    List<Case> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    @Query("select c from Case c where c.consultationSubmission.citizenUserId.userId = :userId order by c.createdAt desc")
    List<Case> findAllForCitizen(UUID userId);

    @Query(value = """
            select c.id from Case c
            left join c.consultationSubmission.patient p
            left join c.consultationSubmission.answer ans
            where (:citizenId is null or c.consultationSubmission.citizenUserId.userId = :citizenId)
              and (:status is null or c.status = :status)
              and (:priority is null or ans.preliminaryPriority = :priority)
              and (:province is null or lower(p.province) like lower(concat('%', cast(:province as String), '%')))
              and (:relation is null
                   or (:relation = 'ASSIGNED_TO_ME' and exists
                       (select a.id from Assignation a where a.caseEntity = c
                        and a.professionalUserId.userId = :professionalId))
                   or (:relation = 'NOT_ASSIGNED_TO_ME' and not exists
                       (select a.id from Assignation a where a.caseEntity = c
                        and a.professionalUserId.userId = :professionalId)))
            """,
            countQuery = """
            select count(c) from Case c
            left join c.consultationSubmission.patient p
            left join c.consultationSubmission.answer ans
            where (:citizenId is null or c.consultationSubmission.citizenUserId.userId = :citizenId)
              and (:status is null or c.status = :status)
              and (:priority is null or ans.preliminaryPriority = :priority)
              and (:province is null or lower(p.province) like lower(concat('%', cast(:province as String), '%')))
              and (:relation is null
                   or (:relation = 'ASSIGNED_TO_ME' and exists
                       (select a.id from Assignation a where a.caseEntity = c
                        and a.professionalUserId.userId = :professionalId))
                   or (:relation = 'NOT_ASSIGNED_TO_ME' and not exists
                       (select a.id from Assignation a where a.caseEntity = c
                        and a.professionalUserId.userId = :professionalId)))
            """)
    Page<UUID> findPageIds(@Param("citizenId") UUID citizenId,
                           @Param("professionalId") UUID professionalId,
                           @Param("status") CaseStatus status,
                           @Param("priority") PreliminaryPriority priority,
                           @Param("province") String province,
                           @Param("relation") String relation,
                           Pageable pageable);

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    List<Case> findByIdIn(List<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Case c where c.id = :id")
    Optional<Case> findByIdForUpdate(UUID id);
}
