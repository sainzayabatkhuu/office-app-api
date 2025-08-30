package com.sol.office_app.entity;

import jakarta.persistence.*;

@Entity
public class ReportRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "run_in_background", nullable = false)
    private String runInBackground;  // Values: "VIRTUAL", "BACKGROUND"

    private String canPrint;

    private String canDownload;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String isCanPrint() {
        return canPrint;
    }

    public void setCanPrint(String canPrint) {
        this.canPrint = canPrint;
    }

    public String isCanDownload() {
        return canDownload;
    }

    public void setCanDownload(String canDownload) {
        this.canDownload = canDownload;
    }

    public String getRunInBackground() {
        return runInBackground;
    }

    public void setRunInBackground(String runInBackground) {
        this.runInBackground = runInBackground;
    }
}
