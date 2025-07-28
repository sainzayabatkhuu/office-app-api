package com.sol.office_app.dto;

public record ProfileDTO(
        String username,
        String first_name,
        String last_name,
        String email,
        String font_size,
        String theme_name,
        String userstatus,
        String account_format,
        String date_format,
        String show_dash,
        String alert_on_home,
        String number_mask,
        boolean multi_brnch_access,
        String branch_code,
        String user_language,
        Number expiration_time
) {
}
