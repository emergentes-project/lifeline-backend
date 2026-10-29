package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCaseByIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.AssignCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ChangeCaseStatusCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.CloseCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCasesByFilterQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCaseStatusHistoryByCaseIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetMyCasesQuery;
import com.upc.pe.lifeline.casemanagement.application.ports.ProfessionalEligibilityChecker;
import com.upc.pe.lifeline.casemanagement.domain.services.CaseQueryService;
import com.upc.pe.lifeline.casemanagement.domain.services.CaseCommandService;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CaseResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CaseStatusHistoryResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ChangeCaseStatusResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CloseCaseResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.transform.CaseResourceFromEntityAssembler;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.transform.CaseStatusHistoryResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Cases", description = "Case management")
@SecurityRequirement(name = "bearerAuth")
public class CaseController {
    private final CaseQueryService queries;
    private final CaseCommandService commands;
    private final ProfessionalEligibilityChecker eligibility;

    public CaseController(CaseQueryService queries, CaseCommandService commands,
                          ProfessionalEligibilityChecker eligibility) {
        this.queries = queries;
        this.commands = commands;
        this.eligibility = eligibility;
    }

    @GetMapping
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    public ResponseEntity<List<CaseResource>> filter(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false)
                                                     String assignmentRelation,
                                                     Authentication authentication) {
        requireApproved(authentication);
        var query = new GetCasesByFilterQuery(status, assignmentRelation,
                UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(queries.handle(query).stream()
                .map(CaseResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<CaseResource>> mine(Authentication authentication) {
        var query = new GetMyCasesQuery(UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(queries.handle(query).stream()
                .map(CaseResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseResource> get(@PathVariable UUID caseId, Authentication authentication) {
        var caseEntity = queries.handle(new GetCaseByIdQuery(caseId));
        requireAccess(caseEntity, authentication);
        return ResponseEntity.ok(CaseResourceFromEntityAssembler.toResourceFromEntity(caseEntity));
    }

    @GetMapping("/{caseId}/history")
    public ResponseEntity<List<CaseStatusHistoryResource>> history(@PathVariable UUID caseId,
                                                                    Authentication authentication) {
        requireAccess(queries.handle(new GetCaseByIdQuery(caseId)), authentication);
        var resources = queries.handle(new GetCaseStatusHistoryByCaseIdQuery(caseId)).stream()
                .map(CaseStatusHistoryResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping("/{caseId}/assign")
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    public ResponseEntity<CaseResource> assign(@PathVariable UUID caseId, Authentication authentication) {
        commands.handle(new AssignCaseCommand(caseId, UUID.fromString(authentication.getName())));
        return ResponseEntity.ok(CaseResourceFromEntityAssembler.toResourceFromEntity(
                queries.handle(new GetCaseByIdQuery(caseId))));
    }

    @PatchMapping(value = "/{caseId}/status", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    public ResponseEntity<CaseResource> changeStatus(@PathVariable UUID caseId,
                                                     @RequestBody ChangeCaseStatusResource resource,
                                                     Authentication authentication) {
        commands.handle(new ChangeCaseStatusCommand(caseId, resource.newStatus(),
                UUID.fromString(authentication.getName())));
        return ResponseEntity.ok(CaseResourceFromEntityAssembler.toResourceFromEntity(
                queries.handle(new GetCaseByIdQuery(caseId))));
    }

    @PostMapping(value = "/{caseId}/close", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    public ResponseEntity<CaseResource> close(@PathVariable UUID caseId,
                                              @RequestBody CloseCaseResource resource,
                                              Authentication authentication) {
        commands.handle(new CloseCaseCommand(caseId, resource.observation(),
                UUID.fromString(authentication.getName())));
        return ResponseEntity.ok(CaseResourceFromEntityAssembler.toResourceFromEntity(
                queries.handle(new GetCaseByIdQuery(caseId))));
    }

    private void requireAccess(Case caseEntity,
                               Authentication authentication) {
        boolean professional = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEDICAL_PROFESSIONAL".equals(authority.getAuthority()));
        var citizenUserId = caseEntity.getConsultationSubmission().getCitizenUserId();
        boolean owner = citizenUserId != null && citizenUserId.userId().toString().equals(authentication.getName());
        if (professional) {
            requireApproved(authentication);
        }
        if (!professional && !owner) {
            throw new AccessDeniedException("Case is not accessible to this user");
        }
    }

    private void requireApproved(Authentication authentication) {
        if (!eligibility.isApproved(UUID.fromString(authentication.getName()))) {
            throw new AccessDeniedException("An approved medical profile is required");
        }
    }
}
