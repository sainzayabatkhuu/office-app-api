package com.sol.office.report.repository;

import com.sol.office.report.domain.ExportMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExportMonitorRepository extends JpaRepository<ExportMonitor, Long>, JpaSpecificationExecutor<ExportMonitor> {

    /** [status, exports, records] in the period */
    @Query("""
            select e.exportStatus, count(e), coalesce(sum(e.totalRecords), 0) from ExportMonitor e
            where e.requestedTime >= :from and e.requestedTime < :to group by e.exportStatus""")
    List<Object[]> countByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [function, exports, records] in the period */
    @Query("""
            select e.functionId, count(e), coalesce(sum(e.totalRecords), 0) from ExportMonitor e
            where e.requestedTime >= :from and e.requestedTime < :to group by e.functionId""")
    List<Object[]> countByFunction(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [user, exports, records] in the period */
    @Query("""
            select e.userId, count(e), coalesce(sum(e.totalRecords), 0) from ExportMonitor e
            where e.requestedTime >= :from and e.requestedTime < :to group by e.userId""")
    List<Object[]> countByUser(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
