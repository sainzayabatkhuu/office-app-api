package com.sol.office.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value objects and enums of customer records. They are also the JSON shape of the customer API.
 */
public final class CustomerParts {

    private CustomerParts() {
    }

    public enum CommunicationMode { MOBILE, EMAIL }

    public enum CustomerGender { MALE, FEMALE, OTHER, PREFER_NOT_TO_DISCLOSE }

    public enum ResidentStatus { RESIDENT, NON_RESIDENT }

    public enum KycStatus { NOT_CAPTURED, PENDING, VERIFIED, REJECTED, EXPIRED }

    public enum OwnershipType {
        SOLE_PROPRIETORSHIP, PARTNERSHIP, LLP, PRIVATE_LIMITED, PUBLIC_LIMITED,
        GOVERNMENT, TRUST, SOCIETY, COOPERATIVE, OTHER
    }

    /** Used twice per customer, so the owning entity maps its columns with prefixes. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Address {
        private String addressName;
        private String addressCode;
        private String line1;
        private String line2;
        private String line3;
        private String line4;
        private String pincode;
        /** ISO 3166-1 alpha-2 */
        private String country;

        public Address copy() {
            return new Address(addressName, addressCode, line1, line2, line3, line4, pincode, country);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class IndividualContact {
        @Column(name = "work_phone_isd", length = 5)
        private String workPhoneIsd;
        @Column(name = "work_phone", length = 32)
        private String workPhone;
        @Column(name = "home_phone_isd", length = 5)
        private String homePhoneIsd;
        @Column(name = "home_phone", length = 32)
        private String homePhone;
        @Column(name = "mobile_isd", length = 5)
        private String mobileIsd;
        @Column(name = "mobile", length = 32)
        private String mobile;
        @Column(name = "fax_isd", length = 5)
        private String faxIsd;
        @Column(name = "fax", length = 32)
        private String fax;
        @Column(name = "email", length = 254)
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class CorporateContact {
        @Column(name = "telephone_isd", length = 5)
        private String telephoneIsd;
        @Column(name = "telephone", length = 32)
        private String telephone;
        @Column(name = "mobile_isd", length = 5)
        private String mobileIsd;
        @Column(name = "mobile", length = 32)
        private String mobile;
        @Column(name = "fax_isd", length = 5)
        private String faxIsd;
        @Column(name = "fax", length = 32)
        private String fax;
        @Column(name = "email", length = 254)
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Passport {
        @Column(name = "passport_number", length = 50)
        private String passportNumber;
        @Column(name = "passport_issue_date")
        private LocalDate issueDate;
        @Column(name = "passport_expiry_date")
        private LocalDate expiryDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class KycDetails {
        @Enumerated(EnumType.STRING)
        @Column(name = "kyc_status", length = 20)
        private KycStatus kycStatus;
        @Column(name = "kyc_reference", length = 64)
        private String kycReference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Incorporation {
        @Column(name = "incorporation_date")
        private LocalDate incorporationDate;
        @Column(name = "capital", precision = 20, scale = 2)
        private BigDecimal capital;
        @Column(name = "net_worth", precision = 20, scale = 2)
        private BigDecimal netWorth;
        /** ISO 4217 */
        @Column(name = "currency", length = 3)
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class CorporateKyc {
        @Enumerated(EnumType.STRING)
        @Column(name = "kyc_status", length = 20)
        private KycStatus kycStatus;
        @Column(name = "kyc_reference", length = 64)
        private String kycReference;
        /** National/registration id of the company */
        @Column(name = "national_id", length = 50)
        private String nationalId;
        @Enumerated(EnumType.STRING)
        @Column(name = "ownership_type", length = 30)
        private OwnershipType ownershipType;
        @Column(name = "business_description", length = 2000)
        private String descriptionOfBusiness;
    }
}
