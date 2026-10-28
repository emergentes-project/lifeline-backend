package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService.Reception;
import com.upc.pe.lifeline.casemanagement.domain.services.ConsultationSubmissionQueryService;
import com.upc.pe.lifeline.casemanagement.domain.services.ConsultationPhotoCommandService;
import com.upc.pe.lifeline.casemanagement.domain.services.ConsultationPhotoQueryService;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.UploadConsultationPhotoCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetConsultationPhotoUrlQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.FilterConsultationSubmissionsQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetConsultationSubmissionByIdQuery;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ConsultationSubmissionResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ConsultationSubmissionDetailResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ConsultationPhotoResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ConsultationPhotoUrlResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ReceiveConsultationSubmissionResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.transform.ConsultationSubmissionDetailResourceFromEntityAssembler;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.transform.ReceiveConsultationSubmissionCommandFromResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;
import java.util.NoSuchElementException;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/consultation-submissions", produces = APPLICATION_JSON_VALUE)
public class ConsultationSubmissionController {
    private final ReceiveConsultationSubmissionCommandService reception;
    private final ConsultationSubmissionQueryService queries;
    private final ConsultationPhotoCommandService photoCommands;
    private final ConsultationPhotoQueryService photoQueries;

    public ConsultationSubmissionController(ReceiveConsultationSubmissionCommandService reception,
                                            ConsultationSubmissionQueryService queries,
                                            ConsultationPhotoCommandService photoCommands,
                                            ConsultationPhotoQueryService photoQueries) {
        this.reception = reception;
        this.queries = queries;
        this.photoCommands = photoCommands;
        this.photoQueries = photoQueries;
    }

    @PostMapping(value = "/{consultationId}/photo", consumes = "multipart/form-data")
    public ResponseEntity<ConsultationPhotoResource> uploadPhoto(
            @PathVariable UUID consultationId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestPart("file") MultipartFile file) throws IOException {
        requireGuestKey(consultationId, idempotencyKey);
        var command = new UploadConsultationPhotoCommand(consultationId, file.getBytes(), file.getContentType());
        String storageKey = photoCommands.handle(command);
        return ResponseEntity.ok(new ConsultationPhotoResource(storageKey));
    }

    @GetMapping("/{consultationId}/photo")
    public ResponseEntity<ConsultationPhotoUrlResource> photoUrl(
            @PathVariable UUID consultationId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey) {
        requireGuestKey(consultationId, idempotencyKey);
        return ResponseEntity.ok(new ConsultationPhotoUrlResource(
                photoQueries.handle(new GetConsultationPhotoUrlQuery(consultationId))));
    }

    private void requireGuestKey(UUID consultationId, UUID idempotencyKey) {
        var submission = queries.handle(new GetConsultationSubmissionByIdQuery(consultationId));
        if (!submission.getIdempotencyKey().equals(idempotencyKey)) {
            throw new NoSuchElementException("Consultation submission not found");
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ConsultationSubmissionDetailResource>> filter(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Boolean helpRequested) {
        var resources = queries.handle(new FilterConsultationSubmissionsQuery(province, helpRequested)).stream()
                .map(ConsultationSubmissionDetailResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{consultationId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ConsultationSubmissionDetailResource> get(@PathVariable UUID consultationId,
                                                                      Authentication authentication) {
        var submission = queries.handle(new GetConsultationSubmissionByIdQuery(consultationId));
        boolean professional = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MEDICAL_PROFESSIONAL".equals(authority.getAuthority()));
        boolean owner = submission.getCitizenUserId() != null
                && submission.getCitizenUserId().userId().toString().equals(authentication.getName());
        if (!professional && !owner) {
            throw new AccessDeniedException("Consultation is not accessible to this user");
        }
        return ResponseEntity.ok(ConsultationSubmissionDetailResourceFromEntityAssembler.toResourceFromEntity(submission));
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ConsultationSubmissionResource> receive(
            @RequestBody ReceiveConsultationSubmissionResource resource,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReceiveConsultationSubmissionCommand command =
                ReceiveConsultationSubmissionCommandFromResourceAssembler.toCommandFromResource(resource);
        requireMatchingIdempotencyKey(idempotencyKey, command.idempotencyKey());
        Reception result = reception.handle(command);
        ConsultationSubmissionResource body = new ConsultationSubmissionResource(
                result.submission().getId(),
                result.submission().getIdempotencyKey(),
                result.submission().isHelpRequested());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(body, status);
    }

    private void requireMatchingIdempotencyKey(String header, UUID bodyKey) {
        if (header == null) {
            return;
        }
        try {
            if (!UUID.fromString(header).equals(bodyKey)) {
                throw new IllegalArgumentException("Idempotency-Key header must match the body");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Idempotency-Key header must match the body");
        }
    }
}
