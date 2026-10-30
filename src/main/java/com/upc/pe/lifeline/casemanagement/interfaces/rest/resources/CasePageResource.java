package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.util.List;

public record CasePageResource(List<CaseResource> items, int page, int size,
                               long totalElements, int totalPages) {}
