package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.commands.SignInUserCommand;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.SignInResource;

public class SignInUserCommandFromResourceAssembler {
    public static SignInUserCommand toCommandFromResource(SignInResource resource) {
        return new SignInUserCommand(resource.email(), resource.password());
    }
}
