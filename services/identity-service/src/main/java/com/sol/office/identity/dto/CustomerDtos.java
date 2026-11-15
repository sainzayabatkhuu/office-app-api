package com.sol.office.identity.dto;

import com.sol.office.identity.domain.CustomerParts.Address;
import com.sol.office.identity.domain.CustomerParts.CommunicationMode;
import com.sol.office.identity.domain.CustomerParts.CorporateContact;
import com.sol.office.identity.domain.CustomerParts.CorporateKyc;
import com.sol.office.identity.domain.CustomerParts.CustomerGender;
import com.sol.office.identity.domain.CustomerParts.Incorporation;
import com.sol.office.identity.domain.CustomerParts.IndividualContact;
import com.sol.office.identity.domain.CustomerParts.KycDetails;
import com.sol.office.identity.domain.CustomerParts.Passport;
import com.sol.office.identity.domain.CustomerParts.ResidentStatus;
import com.sol.office.identity.domain.CustomerType;

import java.time.LocalDate;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    /**
     * A customer, same shape as office-app-api's CustomerDTO. Exactly one of {@code individual} (type R) or
     * {@code corporate} (type C) is set. {@code id} is ignored on requests; an empty {@code customerNumber}
     * on create means "generate the next one".
     */
    public record CustomerDto(
            String id,
            CustomerType customerType,
            String customerNumber,
            String fullName,
            String shortName,
            String branchCode,
            String customerCategory,
            String isPrivateCustomer,
            IndividualDetails individual,
            CorporateDetails corporate) {
    }

    public record IndividualDetails(
            String prefix1,
            String prefix2,
            String prefix3,
            String firstName,
            String middleName,
            String lastName,
            IndividualContact contactInfo,
            Address correspondenceAddress,
            Address permanentAddress,
            String sameAsCorrespondenceAddress,
            String language,
            CommunicationMode communicationMode,
            CustomerGender gender,
            String nationalId,
            String birthPlace,
            String birthCountry,
            LocalDate dateOfBirth,
            String mothersMaidenName,
            ResidentStatus residentStatus,
            Passport passport,
            KycDetails kycDetails,
            Boolean staff,
            Boolean minor,
            String guardian) {
    }

    public record CorporateDetails(
            String name,
            CorporateContact contactInfo,
            Address correspondenceAddress,
            Address registrationAddress,
            String sameAsCorrespondenceAddress,
            String language,
            CommunicationMode communicationMode,
            Incorporation incorporation,
            CorporateKyc kyc) {
    }
}
