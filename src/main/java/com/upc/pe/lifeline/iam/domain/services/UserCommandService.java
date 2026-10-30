package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.SignInUserCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateUserCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.SignOutUserCommand;

public interface UserCommandService {
    User handle(RegisterCitizenCommand command);
    User handle(RegisterMedicalProfessionalCommand command);
    User handle(SignInUserCommand command);
    User handle(UpdateUserCommand command);
    void handle(SignOutUserCommand command);
}
