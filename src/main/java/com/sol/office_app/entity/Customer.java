package com.sol.office_app.entity;

import com.sol.office_app.enums.CustomerType;
import jakarta.persistence.*;
import lombok.Data;

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@Entity
public class Customer {
    @Id
    private String id;

    // Identification
    @Enumerated(EnumType.STRING)
    private CustomerType customerType; // Should ideally use enum: Individual-R, Corporate-C, Bank-B
    @Column(length = 9, unique = true, nullable = false)
    private String customerNumber; // CIF Number (max 9 alphanumeric chars)
    private String specialCustomerNumber; // true if special number is generated
    private String crmCustomer; // true if created from Siebel CRM
    // Names
    private String fullName;
    private String shortName; // Optional abbreviated name
    private String branchCode; // Code of the branch maintaining the customer
    private String customerCategory; // From Customer Category Maintenance
    // Flags
    private String isPrivateCustomer;

    /** Generate ID before persisting */
    @PrePersist
    private void generateId() {
        if (id == null && customerType != null && customerNumber != null) {
            this.id = customerType.name().charAt(0) + customerNumber;
        }
    }
}