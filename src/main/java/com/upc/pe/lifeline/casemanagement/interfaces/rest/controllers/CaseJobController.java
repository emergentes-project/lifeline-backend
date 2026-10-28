package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.services.ProcessCaseJobCommandService;
import com.upc.pe.lifeline.casemanagement.domain.services.ProcessCaseJobCommandService.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/case-jobs")
public class CaseJobController {
    static final String WORKER_SUBJECT = "lifeline-worker";

    private final ProcessCaseJobCommandService jobs;

    public CaseJobController(ProcessCaseJobCommandService jobs) {
        this.jobs = jobs;
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<Void> process(@PathVariable UUID jobId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !WORKER_SUBJECT.equals(authentication.getName())) {
            throw new AccessDeniedException("The lifeline-worker service subject is required");
        }
        HttpStatus status = jobs.handle(jobId) == Result.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).build();
    }
}
