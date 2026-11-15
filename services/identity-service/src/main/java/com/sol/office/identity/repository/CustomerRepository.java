package com.sol.office.identity.repository;

import com.sol.office.identity.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {

    boolean existsByCustomerNumber(String customerNumber);

    /** Highest all-digit customer number, for generating the next one. */
    @Query(value = "SELECT MAX(customer_number) FROM customer WHERE customer_number REGEXP '^[0-9]+$'", nativeQuery = true)
    Optional<String> findMaxNumericCustomerNumber();
}
