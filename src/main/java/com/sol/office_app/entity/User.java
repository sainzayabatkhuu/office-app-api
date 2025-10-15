package com.sol.office_app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sol.office_app.common.Auditable;
import jakarta.persistence.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Table(name = "users")
@Entity
@SoftDelete(strategy = SoftDeleteType.DELETED)
@EntityListeners(AuditingEntityListener.class)
public class User extends Auditable implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true, length = 50, nullable = false)
    private String username;

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;
    private boolean enabled;

    // Track last password change
    private LocalDate lastPasswordChangeDate;

    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Set<Role> roles = new HashSet<>();

    public Set<Role> getRoles() {
        return roles != null ? roles : new HashSet<>();
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getPrivileges(roles));
    }

    private List<String> getPrivileges(Collection<Role> roles) {

        List<String> privileges = new ArrayList<>();
        List<Privilege> collection = new ArrayList<>();
        for (Role role : roles) {
            privileges.add(role.getName());
            collection.addAll(role.getPrivileges());
        }
        for (Privilege item : collection) {
            privileges.add(item.getValue());
        }
        return privileges;
    }

    private List<GrantedAuthority> getGrantedAuthorities(List<String> privileges) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void addRole(Optional<Role> role) {
        if(role.isPresent()) {
            if (this.roles == null) {
                this.roles = new HashSet<>();
            }
            this.roles.add(role.get());
        }
    }

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
    private String accountFormat;

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

    private LocalDateTime expirationTime;

    public boolean getIsActive() {
        return enabled;
    }

    public void setIsActive(boolean isActive) {
        this.enabled = isActive;
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

    public String getAccountFormat() {
        return accountFormat;
    }

    public void setAccountFormat(String accountFormat) {
        this.accountFormat = accountFormat;
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

    public boolean isMultiBrnchAccess() {
        return multiBrnchAccess;
    }

    public void setMultiBrnchAccess(Boolean multiBrnchAccess) {
        this.multiBrnchAccess = multiBrnchAccess;
    }

    public boolean isTokenExpired() {
        return tokenExpired;
    }

    public void setTokenExpired(Boolean tokenExpired) {
        this.tokenExpired = tokenExpired;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    @Column(name = "delete_flag", nullable = false)
    private boolean deleteFlag;

    @Column(name = "account_non_locked")
    private boolean accountNonLocked;

    @Column(name = "failed_attempt")
    private int failedAttempt;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    public boolean isDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public int getFailedAttempt() {
        return failedAttempt;
    }

    public void setFailedAttempt(int failedAttempt) {
        this.failedAttempt = failedAttempt;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "branch_code", nullable = false)
    @JsonIgnore
    private Branch branch;

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getLastPasswordChangeDate() {
        return lastPasswordChangeDate;
    }

    public void setLastPasswordChangeDate(LocalDate lastPasswordChangeDate) {
        this.lastPasswordChangeDate = lastPasswordChangeDate;
    }
}