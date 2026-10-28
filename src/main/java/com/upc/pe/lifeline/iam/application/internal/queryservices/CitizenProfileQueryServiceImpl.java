package com.upc.pe.lifeline.iam.application.internal.queryservices;

import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.queries.GetCitizenProfileByUserIdQuery;
import com.upc.pe.lifeline.iam.domain.services.CitizenProfileQueryService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.CitizenProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CitizenProfileQueryServiceImpl implements CitizenProfileQueryService {
    private final CitizenProfileRepository citizenProfileRepository;

    public CitizenProfileQueryServiceImpl(CitizenProfileRepository citizenProfileRepository) {
        this.citizenProfileRepository = citizenProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CitizenProfile handle(GetCitizenProfileByUserIdQuery query) {
        return citizenProfileRepository.findByUser_Id(query.userId())
                .orElseThrow(() -> new NoSuchElementException("Citizen profile not found"));
    }
}
