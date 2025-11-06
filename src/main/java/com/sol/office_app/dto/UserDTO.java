package com.sol.office_app.dto;

import jakarta.persistence.Column;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserDTO {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String isActive;
    @Column
    private String firstname;
    @Column
    private String lastname;
    @Column
    private String userLanguage;
    @Column
    private String fontSize;
    @Column
    private String themeName;
    @Column
    private String amountFormat;
    @Column
    private String dateFormat;
    @Column
    private String showDash;
    @Column
    private String alertOnHome;
    @Column
    private String numberMask;
    @Column
    private Boolean multiBrnchAccess;
    @Column
    private Boolean tokenExpired;
    private Number expirationTime;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDTO(Long id, String email, String username, String fullName, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.authorities = authorities;
    }

    public UserDTO() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getAmountFormat() {
        return amountFormat;
    }

    public void setAmountFormat(String amountFormat) {
        this.amountFormat = amountFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getShowDash() {
        return showDash;
    }

    public void setShowDash(String showDash) {
        this.showDash = showDash;
    }

    public String getAlertOnHome() {
        return alertOnHome;
    }

    public void setAlertOnHome(String alertOnHome) {
        this.alertOnHome = alertOnHome;
    }

    public String getNumberMask() {
        return numberMask;
    }

    public void setNumberMask(String numberMask) {
        this.numberMask = numberMask;
    }

    public Boolean getMultiBrnchAccess() {
        return multiBrnchAccess;
    }

    public void setMultiBrnchAccess(Boolean multiBrnchAccess) {
        this.multiBrnchAccess = multiBrnchAccess;
    }

    public Boolean getTokenExpired() {
        return tokenExpired;
    }

    public void setTokenExpired(Boolean tokenExpired) {
        this.tokenExpired = tokenExpired;
    }

    public Number getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Number expirationTime) {
        this.expirationTime = expirationTime;
    }
}
