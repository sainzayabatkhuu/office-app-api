package com.sol.office_app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class IndividualCustomer extends Customer {

    // Prefixes and Names
    private String prefix1;
    private String prefix2;
    private String prefix3;
    private String firstName;
    private String middleName;
    private String lastName;

    // Contact Information
    private String workPhoneISD;
    private String workPhone;
    private String homePhoneISD;
    private String homePhone;
    private String mobileISD;
    private String mobileNumber;
    private String faxISD;
    private String faxNumber;
    private String email;
    private String gender;
    private String communicationMode;

    // Identity & Demographics
    private String nationalId;
    private String birthPlace;
    private String birthCountry;
    private LocalDate dateOfBirth;
    private String mothersMaidenName;
    private String language;
    private String nationality;
    private String residentStatus;
    private boolean usResident;
    private boolean visitedUSLast3Years;
    private boolean mfiCustomer;
    private boolean minor;
    private String guardian;
    private String submitAgeProof;

    // Address - Correspondence
    private String corrName;
    private String corrAddress1;
    private String corrAddress2;
    private String corrAddress3;
    private String corrAddress4;
    private String corrPincode;
    private String corrCountry;

    // Address - Permanent
    private String permAddress1;
    private String permAddress2;
    private String permAddress3;
    private String permAddress4;
    private String permPincode;
    private String permCountry;
    private boolean sameAsCorrespondence;

    // Passport
    private String passportNumber;
    private LocalDate passportIssueDate;
    private LocalDate passportExpiryDate;

    // KYC
    private boolean staff;
    private String kycStatus;
    private String kycReference;

    // Preferred Contact Time
    private LocalDate preferredContactDate;
    private String preferredContactTime;

    // Power of Attorney
    private boolean powerOfAttorney;
    private String attorneyHolderName;
    private String attorneyNationality;
    private String attorneyAddress;
    private String attorneyCountry;
    private String attorneyTelephoneISD;
    private String attorneyTelephoneNumber;
}
