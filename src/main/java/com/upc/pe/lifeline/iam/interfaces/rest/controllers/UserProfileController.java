package com.upc.pe.lifeline.iam.interfaces.rest.controllers;

import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.iam.domain.services.UserQueryService;
import com.upc.pe.lifeline.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UpdateUserResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UserResource;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.UpdateUserCommandFromResourceAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Account profile")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserProfileController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my account")
    public ResponseEntity<UserResource> get(Principal principal) {
        var user = userQueryService.handle(new GetUserByIdQuery(UUID.fromString(principal.getName())));
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }

    @PutMapping(value = "/me", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Update my name and phone")
    public ResponseEntity<UserResource> update(@RequestBody UpdateUserResource resource, Principal principal) {
        var command = UpdateUserCommandFromResourceAssembler.toCommandFromResource(
                UUID.fromString(principal.getName()), resource);
        var user = userCommandService.handle(command);
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }
}
