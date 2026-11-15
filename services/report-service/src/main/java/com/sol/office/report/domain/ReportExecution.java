package com.sol.office.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * One run of a report: the request, its state and where the output is. Title/code are copied so history
 * stays readable even if the definition changes later.
 */
@Getter
@Setter
@Entity
@Table(name = "report_execution")
public class ReportExecution {

    @Id
    @Column(length = 36, columnDefinition = "char(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private ReportDefinition report;

    @Column(name = "report_code", nullable = false, length = 50)
    private String reportCode;

    @Column(name = "report_title", nullable = false, length = 200)
    private String reportTitle;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "output_format", nullable = false, length = 10)
    private ReportFormat format;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "execution_mode", nullable = false, length = 10)
    private ExecutionMode mode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    /** Raw parameter values as submitted (strings), plus resolved system parameters. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> parameters;

    @Column(name = "output_storage_key", length = 500)
    private String outputStorageKey;

    @Column(name = "output_file_name")
    private String outputFileName;

    @Column(name = "output_size")
    private Long outputSize;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Version
    @Column(nullable = false)
    private Long version;
}
