package com.upc.pe.lifeline.casemanagement.domain.model.entities;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class Patient extends AuditableModel {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private ConsultationSubmission consultationSubmission;

    @Column(length = 150)
    private String name;

    private Integer ageYears;

    @Column(columnDefinition = "text")
    private String locationDescription;

    @Column(length = 120)
    private String province;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    public Patient() {}

    public Patient(ConsultationSubmission consultationSubmission, String name,
                   Integer ageYears, String locationDescription, String province,
                   BigDecimal latitude, BigDecimal longitude) {
        this.consultationSubmission = consultationSubmission;
        this.name = name;
        this.ageYears = ageYears;
        this.locationDescription = locationDescription;
        this.province = province;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
