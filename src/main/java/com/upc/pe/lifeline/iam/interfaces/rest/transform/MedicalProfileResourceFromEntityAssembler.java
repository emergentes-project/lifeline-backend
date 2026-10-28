package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.MedicalProfileResource;

public class MedicalProfileResourceFromEntityAssembler {
    public static MedicalProfileResource toResourceFromEntity(MedicalProfile profile) {
        return new MedicalProfileResource(profile.getId(), profile.getUser().getId(),
                profile.getProfessionalCategory().value(), profile.getProfessionalLicenseNumber(),
                profile.getSpecialty(), profile.getVerificationStatus(), profile.getVerifiedAt());
    }
}
