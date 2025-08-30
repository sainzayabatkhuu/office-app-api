package com.sol.office_app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sol.office_app.enums.ReportStatus;

import java.time.LocalDateTime;

public class ReportHistoryDto {
    private Long id;
    private String executedBy;
    private String branchCode;
    private String reportFileName;
    private String reportTitleName;
    private String parameters;
    private String reportOutputFile;
    private String runInBackground;
    private ReportStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
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

    public String getRunInBackground() {
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

    // getters/setters
}
