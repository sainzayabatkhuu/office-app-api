package com.sol.office_app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.processing.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class CorporateCustomer extends Customer {

    /**
     * Customer legal/communication name. Stored in full, display truncated to 35.
     */
    @NotBlank
    @Size(max = 140)
    //@Pattern(regexp = SWIFT_REGEX, message = "Name contains non‑SWIFT characters")
    @Column(name = "name", length = 140, nullable = false)
    private String name;

    /**
     * Address for correspondence (mailing).
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "reg_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "corr_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "corr_addr_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "corr_addr_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "corr_addr_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "corr_addr_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "corr_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "corr_country", length = 2))
    })
    private Address correspondenceAddress;

    /** Contact details used for correspondence. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "telephoneIsd", column = @Column(name = "tel_isd", length = 5)),
            @AttributeOverride(name = "telephone", column = @Column(name = "tel_number", length = 32)),
            @AttributeOverride(name = "mobileIsd", column = @Column(name = "mob_isd", length = 5)),
            @AttributeOverride(name = "mobile", column = @Column(name = "mob_number", length = 32)),
            @AttributeOverride(name = "faxIsd", column = @Column(name = "fax_isd", length = 5)),
            @AttributeOverride(name = "fax", column = @Column(name = "fax_number", length = 32)),
            @AttributeOverride(name = "email", column = @Column(name = "email", length = 254))
    })
    private ContactInfo contactInfo;

    /** Language of correspondence (ISO 639-1, e.g. "mn"). */
    @Builder.Default
    @NotBlank
    @Size(min = 2, max = 5)
    @Column(name = "language", length = 5, nullable = false)
    private String language = "mn";

    /** Preferred communication mode (for NEFT products). */
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_mode", length = 16)
    private CommunicationMode communicationMode;

    /** Registration address (aka permanent/registered office). */
    @Builder.Default
    @Column(name = "same_as_corr_addr", nullable = false, length = 1)
    private String sameAsCorrespondenceAddress = "N";

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressName", column = @Column(name = "reg_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "reg_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "reg_addr_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "reg_addr_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "reg_addr_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "reg_addr_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "reg_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "reg_country", length = 2))
    })
    private Address registrationAddress;

    /** Incorporation details. */
    @Embedded
    private Incorporation incorporation;

    /** KYC/Additional details. */
    @Embedded
    private KycAdditionalDetails kyc;

    /** Allowed SWIFT character set for names/addresses (conservative). */
    public static final String SWIFT_REGEX = "[A-Za-z0-9/?\\:().,'+\\- ]*";

    /* ===================== Value Objects ===================== */

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Address {
        @Size(max = 35)
        private String addressName;
        /** Free-form code from reference list. */
        @Size(max = 30)
        //@Pattern(regexp = SWIFT_REGEX, message = "Address code contains non‑SWIFT characters")
        private String addressCode;

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
    public static class ContactInfo {
        @Size(max = 5)
        private String telephoneIsd; // e.g. "+1"

        @Size(max = 32)
        //@Pattern(regexp = "[0-9\\-() ]*", message = "Telephone number has invalid characters")
        private String telephone;

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
    public static class Incorporation {
        private LocalDate incorporationDate;

        @Digits(integer = 18, fraction = 2)
        @Column(precision = 20, scale = 2)
        private BigDecimal capital;

        @Digits(integer = 18, fraction = 2)
        @Column(name = "net_worth", precision = 20, scale = 2)
        private BigDecimal netWorth;

        /** Country of registration (ISO 3166-1 alpha-2). */
//        @Size(min = 2, max = 2)
//        private String country;

        /** ISO 4217 currency code used for financial amounts. */
        @Size(min = 3, max = 3)
        private String currency;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KycAdditionalDetails {
        @Enumerated(EnumType.STRING)
        @Column(name = "kyc_status", length = 20)
        private KycStatus kycStatus;

        @Size(max = 64)
        @Column(name = "kyc_reference", length = 64)
        private String kycReference;

        /** National/Registration ID (e.g., EIN/CIN). */
        @Size(max = 50)
        @Column(name = "national_id", length = 50)
        private String nationalId;

        @Enumerated(EnumType.STRING)
        @Column(name = "ownership_type", length = 30)
        private OwnershipType ownershipType;

        /** Preferred date/time to contact. */
        private LocalDate preferredDateOfContact;

        /** If a strict slot list is needed, map to TimeSlot enum instead of LocalTime. */
        private LocalTime preferredTimeOfContact;

        @Builder.Default
        private boolean jointVenture = false;

        @Size(max = 2000)
//        @Pattern(regexp = SWIFT_REGEX, message = "Description contains non‑SWIFT characters")
        @Column(name = "business_description", length = 2000)
        private String descriptionOfBusiness;
    }

    /* ===================== Enums ===================== */

    public enum CommunicationMode { MOBILE, EMAIL }

    public enum KycStatus { NOT_CAPTURED, PENDING, VERIFIED, REJECTED, EXPIRED }

    public enum OwnershipType {
        SOLE_PROPRIETORSHIP,
        PARTNERSHIP,
        LLP,
        PRIVATE_LIMITED,
        PUBLIC_LIMITED,
        GOVERNMENT,
        TRUST,
        SOCIETY,
        COOPERATIVE,
        OTHER
    }
}
