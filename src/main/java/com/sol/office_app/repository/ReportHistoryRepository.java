package com.sol.office_app.repository;

import com.sol.office_app.entity.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportHistoryRepository extends
        JpaRepository<ReportHistory, Long>,
        JpaSpecificationExecutor<ReportHistory> {
}
