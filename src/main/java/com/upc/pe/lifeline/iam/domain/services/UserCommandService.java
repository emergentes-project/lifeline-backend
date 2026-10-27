package com.upc.pe.lifeline.iam.domain.services;

import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.SignInUserCommand;

public interface UserCommandService {
    User handle(RegisterCitizenCommand command);
    User handle(RegisterMedicalProfessionalCommand command);
    User handle(SignInUserCommand command);
}
