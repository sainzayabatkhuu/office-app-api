package com.sol.office_app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ExportMonitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", unique = true)
    private String referenceNumber;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "function_id")
    private String functionId;

    @Column(name = "fetch_size")
    private Integer fetchSize;

    @Column(name = "requested_time")
    private LocalDateTime requestedTime;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Column(name = "export_status")
    private String exportStatus; // CREATED, PENDING, FAILED

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_records")
    private Integer totalRecords;

}
