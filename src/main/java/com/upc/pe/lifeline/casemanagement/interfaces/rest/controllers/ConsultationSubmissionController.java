package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.UploadConsultationPhotoCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService.Reception;
import com.upc.pe.lifeline.casemanagement.domain.services.UploadConsultationPhotoCommandService;
import com.upc.pe.lifeline.casemanagement.domain.services.UploadConsultationPhotoCommandService.PhotoUpload;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ConsultationSubmissionResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ReceiveConsultationSubmissionResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.transform.ReceiveConsultationSubmissionCommandFromResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/consultation-submissions", produces = APPLICATION_JSON_VALUE)
public class ConsultationSubmissionController {
    private final ReceiveConsultationSubmissionCommandService reception;
    private final UploadConsultationPhotoCommandService photoUpload;

    public ConsultationSubmissionController(ReceiveConsultationSubmissionCommandService reception,
                                            UploadConsultationPhotoCommandService photoUpload) {
        this.reception = reception;
        this.photoUpload = photoUpload;
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

    @PostMapping(value = "/{consultationId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> uploadPhoto(@PathVariable UUID consultationId,
                                              @RequestPart("file") MultipartFile file) throws IOException {
        PhotoUpload result = photoUpload.handle(new UploadConsultationPhotoCommand(
                consultationId, file.getBytes(), file.getContentType()));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(result.storageKey(), status);
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
