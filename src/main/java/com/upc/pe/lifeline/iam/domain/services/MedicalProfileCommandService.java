package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateMedicalProfileCommand;

public interface MedicalProfileCommandService {
    MedicalProfile handle(UpdateMedicalProfileCommand command);
}
