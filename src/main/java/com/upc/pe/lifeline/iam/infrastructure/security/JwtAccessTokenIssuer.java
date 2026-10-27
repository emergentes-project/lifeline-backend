package com.upc.pe.lifeline.iam.infrastructure.security;

import com.upc.pe.lifeline.iam.application.ports.AccessTokenIssuer;
import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtService jwtService;

    public JwtAccessTokenIssuer(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String issue(User user) {
        return jwtService.generateToken(new UserId(user.getId()), user.getRole().getName().name());
    }
}
