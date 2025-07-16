package com.sol.office_app.repository;

import com.sol.office_app.entity.SecurityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityRuleRepository extends JpaRepository<SecurityRule, Long> {
}
