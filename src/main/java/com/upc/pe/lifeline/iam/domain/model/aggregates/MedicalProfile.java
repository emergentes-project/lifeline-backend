package com.upc.pe.lifeline.iam.domain.model.aggregates;

import com.upc.pe.lifeline.iam.domain.model.valueobjects.ProfessionalCategory;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class MedicalProfile extends AuditableAbstractAggregateRoot<MedicalProfile> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProfessionalCategory professionalCategory;

    @Column(nullable = false, unique = true, length = 80)
    private String professionalLicenseNumber;

    @Column(length = 120)
    private String specialty;

    @Column(nullable = false, length = 20)
    private String verificationStatus;

    private LocalDateTime verifiedAt;

    public MedicalProfile() {}

    public MedicalProfile(User user, String professionalCategory,
                          String professionalLicenseNumber, String specialty) {
        this.user = user;
        this.professionalCategory = ProfessionalCategory.from(professionalCategory);
        this.professionalLicenseNumber = professionalLicenseNumber;
        this.specialty = specialty;
        this.verificationStatus = "PENDING";
    }
}
