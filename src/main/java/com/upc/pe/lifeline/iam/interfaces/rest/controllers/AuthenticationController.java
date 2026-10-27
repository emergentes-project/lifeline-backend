package com.upc.pe.lifeline.iam.interfaces.rest.controllers;

import com.upc.pe.lifeline.iam.application.ports.AccessTokenIssuer;
import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.RegisterCitizenResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.RegisterMedicalProfessionalResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.SignInResource;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UserResource;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.RegisterCitizenCommandFromResourceAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.RegisterMedicalProfessionalCommandFromResourceAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.SignInUserCommandFromResourceAssembler;
import com.upc.pe.lifeline.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Citizen and medical professional registration and sign in")
public class AuthenticationController {
    private final UserCommandService userCommandService;
    private final AccessTokenIssuer accessTokenIssuer;

    public AuthenticationController(UserCommandService userCommandService, AccessTokenIssuer accessTokenIssuer) {
        this.userCommandService = userCommandService;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    @PostMapping(value = "/sign-up/citizens", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a citizen")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Citizen registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data"),
            @ApiResponse(responseCode = "409", description = "Email already registered")})
    public ResponseEntity<UserResource> registerCitizen(@RequestBody RegisterCitizenResource resource) {
        var registerCitizenCommand = RegisterCitizenCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = userCommandService.handle(registerCitizenCommand);
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }

    @PostMapping(value = "/sign-up/medical-professionals", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a medical professional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Professional registered with pending verification"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data"),
            @ApiResponse(responseCode = "409", description = "Email or license number already registered")})
    public ResponseEntity<UserResource> registerMedicalProfessional(
            @RequestBody RegisterMedicalProfessionalResource resource) {
        var registerMedicalProfessionalCommand =
                RegisterMedicalProfessionalCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = userCommandService.handle(registerMedicalProfessionalCommand);
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }

    @PostMapping(value = "/sign-in", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Sign in as a citizen or medical professional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid sign in data"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")})
    public ResponseEntity<AuthenticatedUserResource> signIn(@RequestBody SignInResource resource) {
        var signInCommand = SignInUserCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = userCommandService.handle(signInCommand);
        var token = accessTokenIssuer.issue(user);
        var authenticatedUserResource =
                AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(user, token);
        return ResponseEntity.ok(authenticatedUserResource);
    }
}
