package com.sol.office.identity.domain;

import com.sol.office.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * A person working in the organization. Holds HR data only; sign-in data lives in {@link AppUser},
 * which shares this entity's primary key.
 */
@Getter
@Setter
@Entity
@Table(name = "employee")
public class Employee extends BaseEntity {

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** National registration number (регистрийн дугаар). */
    @Column(name = "register_no", unique = true, length = 20)
    private String registerNo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private JobPosition position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    /** Mongolian convention: "Овог.Нэр" shortened to first letter of last name, e.g. "Б.Сайнзаяа". */
    public String getDisplayName() {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return lastName.charAt(0) + "." + firstName;
    }
}
