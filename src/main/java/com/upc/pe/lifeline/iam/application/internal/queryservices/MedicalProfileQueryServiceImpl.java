package com.upc.pe.lifeline.iam.application.internal.queryservices;

import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.queries.GetMedicalProfileByUserIdQuery;
import com.upc.pe.lifeline.iam.domain.services.MedicalProfileQueryService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class MedicalProfileQueryServiceImpl implements MedicalProfileQueryService {
    private final MedicalProfileRepository medicalProfileRepository;

    public MedicalProfileQueryServiceImpl(MedicalProfileRepository medicalProfileRepository) {
        this.medicalProfileRepository = medicalProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalProfile handle(GetMedicalProfileByUserIdQuery query) {
        return medicalProfileRepository.findByUser_Id(query.userId())
                .orElseThrow(() -> new NoSuchElementException("Medical profile not found"));
    }
}
