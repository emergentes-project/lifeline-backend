package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.RegisterMedicalProfessionalResource;

public class RegisterMedicalProfessionalCommandFromResourceAssembler {
    public static RegisterMedicalProfessionalCommand toCommandFromResource(RegisterMedicalProfessionalResource resource) {
        return new RegisterMedicalProfessionalCommand(resource.email(), resource.password(),
                resource.firstName(), resource.lastName(), resource.phone(), resource.professionalCategory(),
                resource.professionalLicenseNumber(), resource.specialty());
    }
}
