package com.upc.pe.lifeline.iam.domain.model.entities;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesName;
import com.upc.pe.lifeline.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Role extends AuditableModel {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private RolesCode code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 80)
    private RolesName name;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    public Role() {}

    public Role(RolesCode code, RolesName name) {
        this.code = code;
        this.name = name;
    }
}
