package com.sol.office_app.dto;

import com.sol.office_app.entity.CorporateCustomer;
import com.sol.office_app.entity.IndividualCustomer;
import com.sol.office_app.enums.CustomerType;

import java.time.LocalDate;

/**
 * A customer (CIF). Exactly one of {@code individual} (type R) or {@code corporate} (type C) is filled.
 * {@code id} is assigned by the server: type letter + customer number (e.g. R000000123).
 */
public record CustomerDTO(
        String id,
        CustomerType customerType,
        /** 9 characters; generated when empty on create */
        String customerNumber,
        String fullName,
        String shortName,
        String branchCode,
        String customerCategory,
        String isPrivateCustomer,
        IndividualDetails individual,
        CorporateDetails corporate
) {

    public record IndividualDetails(
            String prefix1,
            String prefix2,
            String prefix3,
            String firstName,
            String middleName,
            String lastName,
            IndividualCustomer.ContactInfo contactInfo,
            IndividualCustomer.Address correspondenceAddress,
            IndividualCustomer.Address permanentAddress,
            String sameAsCorrespondenceAddress,
            String language,
            IndividualCustomer.CommunicationMode communicationMode,
            IndividualCustomer.Gender gender,
            String nationalId,
            String birthPlace,
            String birthCountry,
            LocalDate dateOfBirth,
            String mothersMaidenName,
            IndividualCustomer.ResidentStatus residentStatus,
            IndividualCustomer.Passport passport,
            IndividualCustomer.KycDetails kycDetails,
            Boolean staff,
            Boolean minor,
            String guardian
    ) {
    }

    public record CorporateDetails(
            String name,
            CorporateCustomer.ContactInfo contactInfo,
            CorporateCustomer.Address correspondenceAddress,
            CorporateCustomer.Address registrationAddress,
            String sameAsCorrespondenceAddress,
            String language,
            CorporateCustomer.CommunicationMode communicationMode,
            CorporateCustomer.Incorporation incorporation,
            CorporateCustomer.KycAdditionalDetails kyc
    ) {
    }
}
