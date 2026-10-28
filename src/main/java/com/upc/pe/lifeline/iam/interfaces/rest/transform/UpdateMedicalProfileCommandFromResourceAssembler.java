package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.commands.UpdateMedicalProfileCommand;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.UpdateMedicalProfileResource;

import java.util.UUID;

public class UpdateMedicalProfileCommandFromResourceAssembler {
    public static UpdateMedicalProfileCommand toCommandFromResource(UUID userId, UpdateMedicalProfileResource resource) {
        return new UpdateMedicalProfileCommand(userId, resource.professionalCategory(),
                resource.professionalLicenseNumber(), resource.specialty());
    }
}
