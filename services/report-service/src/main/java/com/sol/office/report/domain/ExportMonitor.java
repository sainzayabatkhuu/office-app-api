package com.sol.office.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One Excel export of a grid. Status: PENDING while the workbook is written, then COMPLETED or UNCREATED.
 */
@Getter
@Setter
@Entity
@Table(name = "export_monitor")
public class ExportMonitor {

    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String UNCREATED = "UNCREATED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 20)
    private String referenceNumber;

    /** username of the requester */
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    /** screen the grid belongs to, e.g. STDCIF */
    @Column(name = "function_id", nullable = false, length = 20)
    private String functionId;

    @Column(name = "fetch_size")
    private Integer fetchSize;

    @Column(name = "requested_time", nullable = false)
    private LocalDateTime requestedTime;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Column(name = "export_status", nullable = false, length = 20)
    private String exportStatus;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "total_records")
    private Integer totalRecords;
}
