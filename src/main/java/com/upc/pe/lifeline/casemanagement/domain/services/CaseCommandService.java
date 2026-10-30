package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.AssignCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ChangeCaseStatusCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.CloseCaseCommand;

public interface CaseCommandService {
    Case handle(AssignCaseCommand command);
    Case handle(ChangeCaseStatusCommand command);
    Case handle(CloseCaseCommand command);
}
