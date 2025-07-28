package com.sol.office_app.entity;

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

    @Column(name = "schedule_cron_expression")
    private String scheduleCronExpression;

    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;

    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;

    @Column(name = "status")
    private String status; // consider using an enum if you want type safety
    // e.g., 'PENDING', 'RUNNING', 'SUCCESS', 'FAILED'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
