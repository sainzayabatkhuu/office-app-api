package com.sol.office_app.entity;

import com.sol.office_app.enums.CustomerType;
import jakarta.persistence.*;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identification
    private CustomerType customerType; // Should ideally use enum: Individual, Corporate, Bank
    private String customerNumber; // CIF Number (max 9 alphanumeric chars)
    private Boolean specialCustomerNumber; // true if special number is generated
    private Boolean crmCustomer; // true if created from Siebel CRM

    // Names
    private String fullName;
    private String shortName; // Optional abbreviated name

    // Organization Info
    @ManyToOne
    private Branch branchCode; // Code of the branch maintaining the customer
    private String customerCategory; // From Customer Category Maintenance

    // Flags
    private Boolean isPrivateCustomer;
}
