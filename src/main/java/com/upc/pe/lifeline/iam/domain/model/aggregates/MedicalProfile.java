package com.upc.pe.lifeline.iam.domain.model.aggregates;

import com.upc.pe.lifeline.iam.domain.model.valueobjects.ProfessionalCategory;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateMedicalProfileCommand;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

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

    @Column(nullable = false, length = 80)
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
        this.professionalLicenseNumber = professionalLicenseNumber.strip().toUpperCase(Locale.ROOT);
        this.specialty = specialty;
        this.verificationStatus = "PENDING";
    }

    public MedicalProfile(RegisterMedicalProfessionalCommand command, User user) {
        this.user = user;
        this.professionalCategory = ProfessionalCategory.from(command.professionalCategory());
        this.professionalLicenseNumber = command.professionalLicenseNumber().strip().toUpperCase(Locale.ROOT);
        this.specialty = command.specialty();
        this.verificationStatus = "PENDING";
    }

    public void approveVerification() {
        this.verificationStatus = "APPROVED";
        this.verifiedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void rejectVerification() {
        this.verificationStatus = "REJECTED";
        this.verifiedAt = null;
    }

    public boolean update(UpdateMedicalProfileCommand command) {
        String updatedLicense = command.professionalLicenseNumber().strip().toUpperCase(Locale.ROOT);
        boolean licenseChanged = !this.professionalLicenseNumber.equals(updatedLicense);
        this.professionalCategory = ProfessionalCategory.from(command.professionalCategory());
        this.professionalLicenseNumber = updatedLicense;
        this.specialty = command.specialty();
        if (licenseChanged) {
            this.verificationStatus = "PENDING";
            this.verifiedAt = null;
        }
        return licenseChanged;
    }
}
