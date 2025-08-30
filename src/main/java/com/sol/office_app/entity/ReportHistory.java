package com.sol.office_app.entity;

import com.sol.office_app.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_history")
@EntityListeners(AuditingEntityListener.class)
public class ReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    private String executedBy;

    private String branchCode;

    @Column(name = "report_file_name", nullable = false)
    private String reportFileName;

    @Column(name = "report_title", nullable = false)
    private String reportTitleName;

    @Column(columnDefinition = "json")
    private String parameters; // you can parse this to a Map or POJO as needed

    @Column(name = "report_output_file")
    private String reportOutputFile;

    @Column(name = "run_in_background", nullable = false)
    private String runInBackground;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public void setReportFileName(String reportFileName) {
        this.reportFileName = reportFileName;
    }

    public String getReportTitleName() {
        return reportTitleName;
    }

    public void setReportTitleName(String reportTitleName) {
        this.reportTitleName = reportTitleName;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getReportOutputFile() {
        return reportOutputFile;
    }

    public void setReportOutputFile(String reportOutputFile) {
        this.reportOutputFile = reportOutputFile;
    }

    public String isRunInBackground() {
        return runInBackground;
    }

    public void setRunInBackground(String runInBackground) {
        this.runInBackground = runInBackground;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getRunInBackground() {
        return runInBackground;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }
}
