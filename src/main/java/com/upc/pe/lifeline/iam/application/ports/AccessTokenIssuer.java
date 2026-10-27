package com.upc.pe.lifeline.iam.application.ports;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;

public interface AccessTokenIssuer {
    String issue(User user);
}
