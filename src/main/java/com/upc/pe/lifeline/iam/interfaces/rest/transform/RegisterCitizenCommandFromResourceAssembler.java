package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.RegisterCitizenResource;

public class RegisterCitizenCommandFromResourceAssembler {
    public static RegisterCitizenCommand toCommandFromResource(RegisterCitizenResource resource) {
        return new RegisterCitizenCommand(resource.email(), resource.password(), resource.firstName(),
                resource.lastName(), resource.phone(), resource.city(), resource.district(),
                resource.address(), resource.dateOfBirth(), resource.emergencyContactName(),
                resource.emergencyContactPhone());
    }
}
