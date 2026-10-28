package com.upc.pe.lifeline.iam.domain.model.aggregates;

import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateUserCommand;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@Entity
public class User extends AuditableAbstractAggregateRoot<User> {

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public User() {}

    public User(String email, String password, String firstName,
                String lastName, String phone, Role role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
    }

    public User(RegisterCitizenCommand command, String hashedPassword, Role role) {
        this.email = command.email().strip().toLowerCase(Locale.ROOT);
        this.password = hashedPassword;
        this.firstName = command.firstName().strip();
        this.lastName = command.lastName().strip();
        this.phone = command.phone();
        this.role = role;
    }

    public User(RegisterMedicalProfessionalCommand command, String hashedPassword, Role role) {
        this.email = command.email().strip().toLowerCase(Locale.ROOT);
        this.password = hashedPassword;
        this.firstName = command.firstName().strip();
        this.lastName = command.lastName().strip();
        this.phone = command.phone();
        this.role = role;
    }

    public void update(UpdateUserCommand command) {
        this.firstName = command.firstName().strip();
        this.lastName = command.lastName().strip();
        this.phone = command.phone();
    }
}
