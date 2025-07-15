package com.sol.office_app;

import com.sol.office_app.dto.CustomerDTO;
import com.sol.office_app.entity.CorporateCustomer;
import com.sol.office_app.entity.Customer;
import com.sol.office_app.entity.IndividualCustomer;

public class CustomerFactory {
    public static Customer createCustomer(String type, CustomerDTO dto) {
        if ("CORPORATE".equalsIgnoreCase(type)) {
            return createCorporateCustomer(dto);
        } else if ("INDIVIDUAL".equalsIgnoreCase(type)) {
            return createIndividualCustomer(dto);
        } else {
            throw new IllegalArgumentException("Unsupported customer type: " + type);
        }
    }

    private static IndividualCustomer createIndividualCustomer(CustomerDTO dto) {
        IndividualCustomer customer = new IndividualCustomer();
        // set shared fields
        return customer;
    }

    private static CorporateCustomer createCorporateCustomer(CustomerDTO dto) {
        CorporateCustomer corp = new CorporateCustomer();
        // set shared fields
        return corp;
    }
}
