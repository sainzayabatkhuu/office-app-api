package com.sol.office.report.repository;

import com.sol.office.report.domain.ExecutionStatus;
import com.sol.office.report.domain.ReportExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReportExecutionRepository extends JpaRepository<ReportExecution, String> {

    @Query("""
            select e from ReportExecution e
            where (:requestedBy is null or e.requestedBy = :requestedBy)
              and (:reportCode is null or e.reportCode = :reportCode)
              and (:status is null or e.status = :status)
              and (:from is null or e.requestedAt >= :from)
              and (:to is null or e.requestedAt < :to)
            """)
    Page<ReportExecution> search(@Param("requestedBy") String requestedBy,
                                 @Param("reportCode") String reportCode,
                                 @Param("status") ExecutionStatus status,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to,
                                 Pageable pageable);

    List<ReportExecution> findTop200ByStatusAndFinishedAtBefore(ExecutionStatus status, Instant before);

    /** Executions left PROCESSING/PENDING by a crashed instance. */
    @Modifying
    @Query("""
            update ReportExecution e set e.status = :failed, e.errorMessage = :message, e.finishedAt = :now,
                   e.version = e.version + 1
            where e.status in :running and e.requestedAt < :before
            """)
    int failStale(@Param("running") Collection<ExecutionStatus> running,
                  @Param("failed") ExecutionStatus failed,
                  @Param("before") Instant before,
                  @Param("now") Instant now,
                  @Param("message") String message);
}
