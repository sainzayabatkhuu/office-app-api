package com.sol.office.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class UserPreferences {

    @Column(name = "pref_language", nullable = false, length = 5)
    private String language = "mn";

    @Column(name = "pref_theme", length = 30)
    private String theme;

    @Column(name = "pref_date_format", length = 20)
    private String dateFormat;

    @Column(name = "pref_font_size", length = 10)
    private String fontSize;
}
