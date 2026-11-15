package com.sol.office.identity.domain;

import com.sol.office.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A customer (CIF, screen STDCIF). The id is the type letter + customer number (e.g. {@code R000000001}),
 * set by the service before the first save. Details are in {@link IndividualCustomer} / {@link CorporateCustomer}.
 */
@Getter
@Setter
@Entity
@Table(name = "customer")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Customer extends AuditableEntity {

    @Id
    @Column(length = 10)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 1)
    private CustomerType customerType;

    @Column(name = "customer_number", nullable = false, unique = true, length = 9)
    private String customerNumber;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "customer_category", length = 30)
    private String customerCategory;

    @Column(name = "is_private_customer", length = 1)
    private String isPrivateCustomer;
}
