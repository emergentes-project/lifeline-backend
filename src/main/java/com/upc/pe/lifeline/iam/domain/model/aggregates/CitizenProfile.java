package com.upc.pe.lifeline.iam.domain.model.aggregates;

import com.upc.pe.lifeline.iam.domain.model.valueobjects.EmergencyContact;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class CitizenProfile extends AuditableAbstractAggregateRoot<CitizenProfile> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String district;

    @Column(nullable = false, columnDefinition = "text")
    private String address;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(columnDefinition = "text")
    private String preExistingConditions;

    @Column(columnDefinition = "text")
    private String allergies;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "emergency_contact_name", length = 200)),
            @AttributeOverride(name = "phone", column = @Column(name = "emergency_contact_phone", length = 30))
    })
    private EmergencyContact emergencyContact;

    public CitizenProfile() {}

    public CitizenProfile(User user, String city, String district, String address,
                          LocalDate dateOfBirth, String preExistingConditions,
                          String allergies, EmergencyContact emergencyContact) {
        this.user = user;
        this.city = city;
        this.district = district;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.preExistingConditions = preExistingConditions;
        this.allergies = allergies;
        this.emergencyContact = emergencyContact;
    }
}
