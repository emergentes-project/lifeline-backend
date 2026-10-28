package com.upc.pe.lifeline.iam.interfaces.rest.transform;

import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.EmergencyContact;
import com.upc.pe.lifeline.iam.interfaces.rest.resources.CitizenProfileResource;

public class CitizenProfileResourceFromEntityAssembler {
    public static CitizenProfileResource toResourceFromEntity(CitizenProfile profile) {
        EmergencyContact contact = profile.getEmergencyContact();
        return new CitizenProfileResource(profile.getId(), profile.getUser().getId(), profile.getCity(),
                profile.getDistrict(), profile.getAddress(), profile.getDateOfBirth(),
                contact == null ? null : contact.name(), contact == null ? null : contact.phone(),
                profile.getPreExistingConditions(), profile.getAllergies());
    }
}
