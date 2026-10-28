package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.queries.GetMedicalProfileByUserIdQuery;

public interface MedicalProfileQueryService {
    MedicalProfile handle(GetMedicalProfileByUserIdQuery query);
}
