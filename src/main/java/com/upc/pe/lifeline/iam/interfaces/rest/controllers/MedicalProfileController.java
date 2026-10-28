package com.upc.pe.lifeline.iam.interfaces.rest.controllers;

import com.upc.pe.lifeline.iam.domain.services.MedicalProfileCommandService;
import com.upc.pe.lifeline.iam.domain.services.MedicalProfileQueryService;
import com.upc.pe.lifeline.iam.domain.model.queries.GetMedicalProfileByUserIdQuery;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.MedicalProfileResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UpdateMedicalProfileResource;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.MedicalProfileResourceFromEntityAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.UpdateMedicalProfileCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/medical-profiles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Medical Profiles", description = "Medical professional profile management")
@SecurityRequirement(name = "bearerAuth")
public class MedicalProfileController {
    private final MedicalProfileCommandService medicalProfileCommandService;
    private final MedicalProfileQueryService medicalProfileQueryService;

    public MedicalProfileController(MedicalProfileCommandService medicalProfileCommandService,
                                    MedicalProfileQueryService medicalProfileQueryService) {
        this.medicalProfileCommandService = medicalProfileCommandService;
        this.medicalProfileQueryService = medicalProfileQueryService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    @Operation(summary = "Get my medical profile")
    public ResponseEntity<MedicalProfileResource> get(Principal principal) {
        var query = new GetMedicalProfileByUserIdQuery(UUID.fromString(principal.getName()));
        var profile = medicalProfileQueryService.handle(query);
        return ResponseEntity.ok(MedicalProfileResourceFromEntityAssembler.toResourceFromEntity(profile));
    }

    @PutMapping(value = "/me", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    @Operation(summary = "Update my medical profile and reverify a changed license")
    public ResponseEntity<MedicalProfileResource> update(@RequestBody UpdateMedicalProfileResource resource,
                                                         Principal principal) {
        var command = UpdateMedicalProfileCommandFromResourceAssembler.toCommandFromResource(
                UUID.fromString(principal.getName()), resource);
        medicalProfileCommandService.handle(command);
        var profile = medicalProfileQueryService.handle(new GetMedicalProfileByUserIdQuery(command.userId()));
        return ResponseEntity.ok(MedicalProfileResourceFromEntityAssembler.toResourceFromEntity(profile));
    }
}
