package com.upc.pe.lifeline.iam.application.internal.queryservices;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.pe.lifeline.iam.domain.services.UserQueryService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
