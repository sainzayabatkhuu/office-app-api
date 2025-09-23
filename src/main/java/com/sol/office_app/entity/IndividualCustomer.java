package com.sol.office_app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class IndividualCustomer extends Customer {

    // Prefixes and Names
    @Size(max = 20)
    @Column(name = "prefix1", length = 20)
    private String prefix1;

    @Size(max = 20)
    @Column(name = "prefix2", length = 20)
    private String prefix2;

    @Size(max = 20)
    @Column(name = "prefix3", length = 20)
    private String prefix3;

    @Size(max = 100)
    @NotBlank
    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Size(max = 100)
    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Size(max = 100)
    @NotBlank
    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;


    /** Contact details used for correspondence. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "workPhoneIsd", column = @Column(name = "work_phone_isd", length = 5)),
            @AttributeOverride(name = "workPhone", column = @Column(name = "work_phone", length = 32)),
            @AttributeOverride(name = "homePhoneIsd", column = @Column(name = "home_phone_isd", length = 5)),
            @AttributeOverride(name = "homePhone", column = @Column(name = "home_phone", length = 32)),
            @AttributeOverride(name = "mobileIsd", column = @Column(name = "mob_isd", length = 5)),
            @AttributeOverride(name = "mobile", column = @Column(name = "mob_number", length = 32)),
            @AttributeOverride(name = "faxIsd", column = @Column(name = "fax_isd", length = 5)),
            @AttributeOverride(name = "fax", column = @Column(name = "fax_number", length = 32)),
            @AttributeOverride(name = "email", column = @Column(name = "email", length = 254))
    })
    private ContactInfo contactInfo;

    /**
     * Address for correspondence (mailing).
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressCode", column = @Column(name = "corr_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "corr_addr_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "corr_addr_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "corr_addr_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "corr_addr_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "corr_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "corr_country", length = 2))
    })
    private Address correspondenceAddress;

    /** Language of correspondence (ISO 639-1, e.g. "mn"). */
    @Builder.Default
    @NotBlank
    @Size(min = 2, max = 5)
    @Column(name = "language", length = 5, nullable = false)
    private String language = "mn";

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_mode", length = 16)
    private CommunicationMode communicationMode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "reg_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "reg_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "reg_addr_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "reg_addr_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "reg_addr_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "reg_addr_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "reg_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "reg_country", length = 2))
    })
    private Address permanentAddress;

    /** Registration address (aka permanent/registered office). */
    @Builder.Default
    @Column(name = "same_as_corr_addr", nullable = false, length = 1)
    private String sameAsCorrespondenceAddress = "N";

    // Identity & Demographics
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Size(max = 50)
    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Size(max = 200)
    @Column(name = "birth_place", length = 200)
    private String birthPlace;

    @Size(min = 2, max = 2)
    @Column(name = "birth_country", length = 2)
    private String birthCountry;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 200)
    @Column(name = "mothers_maiden_name", length = 200)
    private String mothersMaidenName;

    /* ===== Residency / US related fields ===== */
    @Enumerated(EnumType.STRING)
    @Column(name = "resident_status", length = 20)
    private ResidentStatus residentStatus;

    @Column(name = "permanent_us_resident")
    private Boolean permanentUsResident = false;

    @Column(name = "visited_us_last_3_years")
    private Boolean visitedUsLast3Years = false;

    /* ===== MFI and Passport details ===== */
    @Column(name = "mfi_customer")
    private Boolean mfiCustomer = false;

    @Embedded
    private Passport passport;

    /* ===== KYC and supplementary ===== */
    @Embedded
    private KycDetails kycDetails;

    @Column(name = "staff")
    private Boolean staff = false;

    @Column(name = "guardian")
    private String guardian;

    @Column(name = "minor")
    private Boolean minor = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "submit_age_proof", length = 20)
    private SubmitAgeProof submitAgeProof;

    @Column(name = "preferred_date_of_contact")
    private LocalDate preferredDateOfContact;

    @Column(name = "preferred_time_of_contact", length = 32)
    private String preferredTimeOfContact;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactInfo {
        @Size(max = 5)
        private String workPhoneIsd; // e.g. "+1"

        @Size(max = 32)
        //@Pattern(regexp = "[0-9\\-() ]*", message = "Telephone number has invalid characters")
        private String workPhone;

        @Size(max = 5)
        private String homePhoneIsd; // e.g. "+1"

        @Size(max = 32)
        //@Pattern(regexp = "[0-9\\-() ]*", message = "Telephone number has invalid characters")
        private String homePhone;

        @Size(max = 5)
        private String mobileIsd; // e.g. "+91"

        @Size(max = 32)
        //@Pattern(regexp = "[0-9\\-() ]*", message = "Mobile number has invalid characters")
        private String mobile;

        @Size(max = 5)
        private String faxIsd;

        @Size(max = 32)
//        @Pattern(regexp = "[0-9\\-() ]*", message = "Fax number has invalid characters")
        private String fax;

        @Email
        @Size(max = 254)
        private String email;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Address {

        @Size(max = 35)
        //@Pattern(regexp = SWIFT_REGEX, message = "Address line 1 contains non‑SWIFT characters")
        private String line1;

        @Size(max = 35)
        //@Pattern(regexp = SWIFT_REGEX, message = "Address line 2 contains non‑SWIFT characters")
        private String line2;

        @Size(max = 35)
        //@Pattern(regexp = SWIFT_REGEX, message = "Address line 3 contains non‑SWIFT characters")
        private String line3;

        @Size(max = 35)
        //@Pattern(regexp = SWIFT_REGEX, message = "Address line 4 contains non‑SWIFT characters")
        private String line4;

        @Size(max = 20)
        //@Pattern(regexp = "[A-Za-z0-9\\- ]*", message = "Pincode has invalid characters")
        private String pincode;

        /** ISO 3166-1 alpha-2 (e.g., US, IN, GB). */
        @Size(min = 2, max = 2)
        private String country;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Passport {
        @Size(max = 50)
        @Column(name = "passport_number", length = 50)
        private String passportNumber;

        @Column(name = "passport_issue_date")
        private LocalDate issueDate;

        @Column(name = "passport_expiry_date")
        private LocalDate expiryDate;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KycDetails {
        @Enumerated(EnumType.STRING)
        @Column(name = "kyc_status", length = 20)
        private KycStatus kycStatus;

        @Size(max = 64)
        @Column(name = "kyc_reference", length = 64)
        private String kycReference;

        @Size(max = 2000)
        @Column(name = "business_description", length = 2000)
        private String businessDescription;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PowerOfAttorney {
        @Column(name = "poa_holder_name", length = 200)
        private String holderName;

        @Size(min = 2, max = 2)
        @Column(name = "poa_nationality", length = 2)
        private String nationality;

        @Column(name = "poa_address", length = 500)
        private String address;

        @Size(max = 5)
        @Column(name = "poa_tel_isd", length = 5)
        private String telephoneIsd;

        @Size(max = 32)
        @Column(name = "poa_tel_number", length = 32)
        private String telephoneNumber;
    }

    public enum Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_DISCLOSE }
    public enum ResidentStatus { RESIDENT, NON_RESIDENT }
    public enum CommunicationMode { MOBILE, EMAIL }
    public enum SubmitAgeProof { NOT_REQUIRED, PENDING, VERIFIED }
    public enum KycStatus { NOT_CAPTURED, PENDING, VERIFIED, REJECTED, EXPIRED }
}
