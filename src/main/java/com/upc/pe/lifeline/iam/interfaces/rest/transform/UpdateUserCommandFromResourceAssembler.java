package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.commands.UpdateUserCommand;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UpdateUserResource;

import java.util.UUID;

public class UpdateUserCommandFromResourceAssembler {
    public static UpdateUserCommand toCommandFromResource(UUID userId, UpdateUserResource resource) {
        return new UpdateUserCommand(userId, resource.firstName(), resource.lastName(), resource.phone());
    }
}
