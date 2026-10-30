package com.upc.pe.lifeline.iam.interfaces.rest.controllers;

import com.upc.pe.lifeline.iam.domain.model.queries.GetAllRolesQuery;
import com.upc.pe.lifeline.iam.domain.services.RoleQueryService;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.RoleResource;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.RoleResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/roles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Roles", description = "Available IAM roles")
public class RoleController {
    private final RoleQueryService roleQueryService;

    public RoleController(RoleQueryService roleQueryService) {
        this.roleQueryService = roleQueryService;
    }

    @GetMapping
    @Operation(summary = "Get all available roles")
    public ResponseEntity<List<RoleResource>> getAll() {
        var resources = roleQueryService.handle(new GetAllRolesQuery()).stream()
                .map(RoleResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(resources);
    }
}
