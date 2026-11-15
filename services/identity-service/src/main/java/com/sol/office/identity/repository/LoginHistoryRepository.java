package com.sol.office.identity.repository;

import com.sol.office.identity.domain.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Page<LoginHistory> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
