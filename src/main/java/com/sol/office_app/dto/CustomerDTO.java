package com.sol.office_app.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerDTO(
        Long id,
        String fullname,
        String shortname,
        String branchCode,
        String customerCategory,
        Boolean isPrivateCustomer,

        String prefix1,
        String prefix2,
        String prefix3,
        String firstname,
        String middlename,
        String lastname,
        String workPhoneISD,
        String workPhone,
        String homePhoneISD,
        String homePhone,
        String mobilePhoneISD,
        String mobilePhone,
        String faxISD,
        String faxNumber,
        String email,
        String gender,
        String communicationMode,
        String nationalId,
        String birthPLace,
        LocalDate dateOfBirth,
        String address,
        String city,
        String state,
        String zipcode,
        String country,
        LocalDateTime registrationDate
) { }
