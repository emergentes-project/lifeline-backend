package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.AssignCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ChangeCaseStatusCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.CaseCommandService;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CaseResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ChangeCaseStatusResource;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.transform.CaseResourceFromEntityAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/cases", produces = APPLICATION_JSON_VALUE)
public class CaseController {
    private final CaseCommandService caseCommandService;

    public CaseController(CaseCommandService caseCommandService) {
        this.caseCommandService = caseCommandService;
    }

    @PostMapping("/{caseId}/assign")
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    public ResponseEntity<CaseResource> assign(@PathVariable UUID caseId, Authentication authentication) {
        var command = new AssignCaseCommand(caseId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(CaseResourceFromEntityAssembler.toResourceFromEntity(
                caseCommandService.handle(command)));
    }

    @PatchMapping(value = "/{caseId}/status", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('MEDICAL_PROFESSIONAL')")
    public ResponseEntity<CaseResource> changeStatus(@PathVariable UUID caseId,
                                                     @RequestBody ChangeCaseStatusResource resource,
                                                     Authentication authentication) {
        var command = new ChangeCaseStatusCommand(caseId, resource.newStatus(),
                UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(CaseResourceFromEntityAssembler.toResourceFromEntity(
                caseCommandService.handle(command)));
    }
}
