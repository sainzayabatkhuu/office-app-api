package com.sol.office_app.dto;

import java.time.LocalDate;

public record ProfileDTO(
        String username,
        String firstName,
        String lastName,
        String email,
        String fontSize,
        String themeName,
        String userstatus,
        String amountFormat,
        String dateFormat,
        String showDash,
        String alertOnHome,
        String numberMask,
        boolean multiBrnchAccess,
        String branchCode,
        String userLanguage,
        Number expirationTime,
        LocalDate lastPasswordChangeDate,
        boolean forcePasswordChange
) {
}
