package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.queries.GetUserByIdQuery;

public interface UserQueryService {
    User handle(GetUserByIdQuery query);
}
