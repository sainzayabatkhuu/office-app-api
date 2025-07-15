package com.sol.office_app.entity;

import com.sol.office_app.enums.RunLocation;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String fileName;

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Enumerated(EnumType.STRING)
    private RunLocation runLocation;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportRolePermission> rolePermissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public RunLocation getRunLocation() {
        return runLocation;
    }

    public void setRunLocation(RunLocation runLocation) {
        this.runLocation = runLocation;
    }

    public List<ReportRolePermission> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(List<ReportRolePermission> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }
}
