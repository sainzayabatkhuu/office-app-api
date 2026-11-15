package com.sol.office.identity.domain;

import com.sol.office.identity.domain.CustomerParts.Address;
import com.sol.office.identity.domain.CustomerParts.CommunicationMode;
import com.sol.office.identity.domain.CustomerParts.CustomerGender;
import com.sol.office.identity.domain.CustomerParts.IndividualContact;
import com.sol.office.identity.domain.CustomerParts.KycDetails;
import com.sol.office.identity.domain.CustomerParts.Passport;
import com.sol.office.identity.domain.CustomerParts.ResidentStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Individual (retail, type R) customer. */
@Getter
@Setter
@Entity
@Table(name = "individual_customer")
public class IndividualCustomer extends Customer {

    @Column(length = 20)
    private String prefix1;

    @Column(length = 20)
    private String prefix2;

    @Column(length = 20)
    private String prefix3;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Embedded
    private IndividualContact contactInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressName", column = @Column(name = "corr_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "corr_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "corr_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "corr_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "corr_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "corr_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "corr_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "corr_country", length = 2))
    })
    private Address correspondenceAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressName", column = @Column(name = "perm_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "perm_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "perm_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "perm_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "perm_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "perm_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "perm_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "perm_country", length = 2))
    })
    private Address permanentAddress;

    @Column(name = "same_as_corr_addr", nullable = false, length = 1)
    private String sameAsCorrespondenceAddress = "N";

    /** ISO 639-1, e.g. "mn" */
    @Column(nullable = false, length = 5)
    private String language = "mn";

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_mode", length = 16)
    private CommunicationMode communicationMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CustomerGender gender;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "birth_place", length = 200)
    private String birthPlace;

    @Column(name = "birth_country", length = 2)
    private String birthCountry;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "mothers_maiden_name", length = 200)
    private String mothersMaidenName;

    @Enumerated(EnumType.STRING)
    @Column(name = "resident_status", length = 20)
    private ResidentStatus residentStatus;

    @Embedded
    private Passport passport;

    @Embedded
    private KycDetails kycDetails;

    @Column(nullable = false)
    private boolean staff;

    @Column(nullable = false)
    private boolean minor;

    @Column(length = 200)
    private String guardian;
}
