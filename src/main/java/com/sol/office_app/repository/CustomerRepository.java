package com.sol.office_app.repository;

import com.sol.office_app.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {

    /** Across individual and corporate customers (TABLE_PER_CLASS hierarchy). */
    boolean existsByCustomerNumber(String customerNumber);

    Optional<Customer> findTopByOrderByCustomerNumberDesc();
}
