package com.sol.office.identity.domain;

import com.sol.office.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sign-in account of an {@link Employee}. The primary key IS the employee id ({@code @MapsId}), which gives the
 * same table layout as JOINED inheritance ({@code AppUser extends Employee}) while still allowing an existing
 * employee to be granted an account later — something JPA inheritance cannot do (an entity can't change type).
 */
@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser extends AuditableEntity {

    @Id
    @Column(name = "employee_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Permanently locked; only an administrator can unlock. */
    @Column(nullable = false)
    private boolean locked;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Temporarily locked until this moment after too many failed attempts. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "multi_branch_access", nullable = false)
    private boolean multiBranchAccess;

    @Embedded
    private UserPreferences preferences = new UserPreferences();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();

    public boolean isTemporarilyLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Set<String> roleCodes() {
        Set<String> codes = new LinkedHashSet<>();
        roles.forEach(r -> codes.add(r.getCode()));
        return codes;
    }

    public Set<String> privilegeCodes() {
        Set<String> codes = new HashSet<>();
        roles.forEach(r -> r.getPrivileges().forEach(p -> codes.add(p.getCode())));
        return codes;
    }
}
