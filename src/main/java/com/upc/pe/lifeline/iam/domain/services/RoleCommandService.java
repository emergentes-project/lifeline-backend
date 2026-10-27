package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}
