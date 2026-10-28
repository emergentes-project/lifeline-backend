package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.queries.GetCitizenProfileByUserIdQuery;

public interface CitizenProfileQueryService {
    CitizenProfile handle(GetCitizenProfileByUserIdQuery query);
}
