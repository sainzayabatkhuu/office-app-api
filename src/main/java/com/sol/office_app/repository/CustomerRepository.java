package com.sol.office_app.repository;

import com.sol.office_app.entity.Customer;
import com.sol.office_app.entity.IndividualCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

}
