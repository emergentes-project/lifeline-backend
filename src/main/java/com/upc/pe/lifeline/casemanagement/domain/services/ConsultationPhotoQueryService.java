package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetConsultationPhotoUrlQuery;

public interface ConsultationPhotoQueryService {
    String handle(GetConsultationPhotoUrlQuery query);
}
