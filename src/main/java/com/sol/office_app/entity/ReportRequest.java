package com.sol.office_app.entity;

import com.sol.office_app.enums.ReportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_requests")
public class ReportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Column(columnDefinition = "json")
    private String parameters; // you can parse this to a Map or POJO as needed

    @Column(name = "run_in_background", nullable = false)
    private boolean runInBackground = false;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
