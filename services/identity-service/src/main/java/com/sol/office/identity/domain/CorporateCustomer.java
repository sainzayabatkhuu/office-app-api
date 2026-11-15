package com.sol.office.identity.domain;

import com.sol.office.identity.domain.CustomerParts.Address;
import com.sol.office.identity.domain.CustomerParts.CommunicationMode;
import com.sol.office.identity.domain.CustomerParts.CorporateContact;
import com.sol.office.identity.domain.CustomerParts.CorporateKyc;
import com.sol.office.identity.domain.CustomerParts.Incorporation;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Corporate (type C) customer. */
@Getter
@Setter
@Entity
@Table(name = "corporate_customer")
public class CorporateCustomer extends Customer {

    /** Legal name */
    @Column(nullable = false, length = 140)
    private String name;

    @Embedded
    private CorporateContact contactInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressName", column = @Column(name = "corr_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "corr_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "corr_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "corr_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "corr_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "corr_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "corr_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "corr_country", length = 2))
    })
    private Address correspondenceAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressName", column = @Column(name = "reg_address_name", length = 35)),
            @AttributeOverride(name = "addressCode", column = @Column(name = "reg_address_code", length = 30)),
            @AttributeOverride(name = "line1", column = @Column(name = "reg_line1", length = 35)),
            @AttributeOverride(name = "line2", column = @Column(name = "reg_line2", length = 35)),
            @AttributeOverride(name = "line3", column = @Column(name = "reg_line3", length = 35)),
            @AttributeOverride(name = "line4", column = @Column(name = "reg_line4", length = 35)),
            @AttributeOverride(name = "pincode", column = @Column(name = "reg_pincode", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "reg_country", length = 2))
    })
    private Address registrationAddress;

    @Column(name = "same_as_corr_addr", nullable = false, length = 1)
    private String sameAsCorrespondenceAddress = "N";

    /** ISO 639-1, e.g. "mn" */
    @Column(nullable = false, length = 5)
    private String language = "mn";

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_mode", length = 16)
    private CommunicationMode communicationMode;

    @Embedded
    private Incorporation incorporation;

    @Embedded
    private CorporateKyc kyc;
}
