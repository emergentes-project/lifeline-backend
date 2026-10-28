package com.upc.pe.lifeline.iam.interfaces.rest.controllers;

import com.upc.pe.lifeline.iam.domain.services.CitizenProfileCommandService;
import com.upc.pe.lifeline.iam.domain.services.CitizenProfileQueryService;
import com.upc.pe.lifeline.iam.domain.model.queries.GetCitizenProfileByUserIdQuery;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.CitizenProfileResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UpdateCitizenProfileResource;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.CitizenProfileResourceFromEntityAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.UpdateCitizenProfileCommandFromResourceAssembler;
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
@RequestMapping(value = "/api/v1/citizen-profiles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Citizen Profiles", description = "Citizen profile management")
@SecurityRequirement(name = "bearerAuth")
public class CitizenProfileController {
    private final CitizenProfileCommandService citizenProfileCommandService;
    private final CitizenProfileQueryService citizenProfileQueryService;

    public CitizenProfileController(CitizenProfileCommandService citizenProfileCommandService,
                                    CitizenProfileQueryService citizenProfileQueryService) {
        this.citizenProfileCommandService = citizenProfileCommandService;
        this.citizenProfileQueryService = citizenProfileQueryService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Get my citizen profile")
    public ResponseEntity<CitizenProfileResource> get(Principal principal) {
        var query = new GetCitizenProfileByUserIdQuery(UUID.fromString(principal.getName()));
        var profile = citizenProfileQueryService.handle(query);
        return ResponseEntity.ok(CitizenProfileResourceFromEntityAssembler.toResourceFromEntity(profile));
    }

    @PutMapping(value = "/me", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Update my citizen profile")
    public ResponseEntity<CitizenProfileResource> update(@RequestBody UpdateCitizenProfileResource resource,
                                                         Principal principal) {
        var command = UpdateCitizenProfileCommandFromResourceAssembler.toCommandFromResource(
                UUID.fromString(principal.getName()), resource);
        var profile = citizenProfileCommandService.handle(command);
        return ResponseEntity.ok(CitizenProfileResourceFromEntityAssembler.toResourceFromEntity(profile));
    }
}
