package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.queries.GetAllRolesQuery;

import java.util.List;

public interface RoleQueryService {
    List<Role> handle(GetAllRolesQuery query);
}
