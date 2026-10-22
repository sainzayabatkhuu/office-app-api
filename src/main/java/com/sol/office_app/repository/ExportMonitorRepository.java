package com.sol.office_app.repository;

import com.sol.office_app.entity.ExportMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportMonitorRepository extends
        JpaRepository<ExportMonitor, Long>,
        JpaSpecificationExecutor<ExportMonitor> {
    ExportMonitor findByReferenceNumber(String referenceNumber);

    /** [status, exports, records] in the period */
    @Query("""
            select e.exportStatus, count(e), coalesce(sum(e.totalRecords), 0) from ExportMonitor e
            where e.requestedTime >= :from and e.requestedTime < :to group by e.exportStatus""")
    List<Object[]> countByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select e.functionId, count(e), coalesce(sum(e.totalRecords), 0) from ExportMonitor e
            where e.requestedTime >= :from and e.requestedTime < :to group by e.functionId""")
    List<Object[]> countByFunction(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select e.userId, count(e), coalesce(sum(e.totalRecords), 0) from ExportMonitor e
            where e.requestedTime >= :from and e.requestedTime < :to group by e.userId""")
    List<Object[]> countByUser(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
