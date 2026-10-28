package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.commands.UpdateCitizenProfileCommand;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UpdateCitizenProfileResource;

import java.util.UUID;

public class UpdateCitizenProfileCommandFromResourceAssembler {
    public static UpdateCitizenProfileCommand toCommandFromResource(UUID userId, UpdateCitizenProfileResource resource) {
        return new UpdateCitizenProfileCommand(userId, resource.city(), resource.district(), resource.address(),
                resource.emergencyContactName(), resource.emergencyContactPhone(),
                resource.preExistingConditions(), resource.allergies());
    }
}
