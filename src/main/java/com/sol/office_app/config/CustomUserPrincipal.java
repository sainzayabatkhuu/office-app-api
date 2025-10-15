package com.sol.office_app.config;

import java.security.Principal;
import java.util.*;

public class CustomUserPrincipal implements Principal {
    private final String username;
//    private final String email;
//    private final String userLanguage;
//    private final String fontSize;
//    private final String themeName;
//    private final String accountFormat;
//    private final String dateFormat;
//    private final String showDash;
//    private final String alertOnHome;
//    private final String numberMask;
    private final String branch;
    private final Boolean multiBrnchAccess;
    private final List<String> rolePermissions;

    private final Map<String, List<String>> rules;

    public CustomUserPrincipal(
            String username,
//            String email,
//            String userLanguage,
//            String fontSize,
//            String themeName,
//            String accountFormat,
//            String dateFormat,
//            String showDash,
//            String alertOnHome,
//            String numberMask,
            String branch,
            Boolean multiBrnchAccess,
            List<String> rolePermissions,
            Map<String, List<String>> rules) {
        this.username = username;
//        this.email = email;
//        this.userLanguage = userLanguage;
//        this.fontSize = fontSize;
//        this.themeName = themeName;
//        this.accountFormat = accountFormat;
//        this.dateFormat = dateFormat;
//        this.showDash = showDash;
//        this.alertOnHome = alertOnHome;
//        this.numberMask = numberMask;
        this.branch= branch;
        this.multiBrnchAccess = multiBrnchAccess;
        this.rolePermissions = rolePermissions;
        this.rules = rules;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getBranch() { return this.branch; }

    public Boolean getMultiBrnchAccess() { return this.multiBrnchAccess; }

    public List<String> getRolePermissions() {
        return rolePermissions;
    }

    public Map<String, List<String>> getRules() {return rules;}


}