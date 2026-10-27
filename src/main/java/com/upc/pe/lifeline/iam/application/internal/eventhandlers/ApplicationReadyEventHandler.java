package com.upc.pe.lifeline.iam.application.internal.eventhandlers;

import com.upc.pe.lifeline.iam.domain.model.commands.SeedRolesCommand;
import com.upc.pe.lifeline.iam.domain.services.RoleCommandService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationReadyEventHandler {
    private final RoleCommandService roleCommandService;

    public ApplicationReadyEventHandler(RoleCommandService roleCommandService) {
        this.roleCommandService = roleCommandService;
    }

    @EventListener
    public void on(ApplicationReadyEvent event) {
        roleCommandService.handle(new SeedRolesCommand());
    }
}
