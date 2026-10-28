package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateCitizenProfileCommand;

public interface CitizenProfileCommandService {
    CitizenProfile handle(UpdateCitizenProfileCommand command);
}
