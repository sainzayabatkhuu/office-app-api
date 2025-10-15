package com.sol.office_app.repository;

import com.sol.office_app.entity.ExportMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportMonitorRepository extends JpaRepository<ExportMonitor, Long> {
    ExportMonitor findByReferenceNumber(String referenceNumber);

    List<ExportMonitor> findByFunctionIdContainingAndUserIdContainingAndReferenceNumberContaining(
            String functionId, String userId, String referenceNumber
    );
}
