package com.sol.office_app.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Branch {
    @Id
    private String solId;

    @Column
    private String name;
    @Column
    private String alternateBranchCode;
    @Column
    private Boolean branchAvailable;

    @Column
    private String hostCode;
    @Column
    private String addressLine1;
    @Column
    private String addressLine2;
    @Column
    private String addressLine3;
    @Column(name = "weekly_holiday_1")
    private String weeklyHoliday1;
    @Column(name = "weekly_holiday_2")
    private String weeklyHoliday2;

    @ManyToOne
    private Branch parentBranch;

    @OneToMany(mappedBy = "parentBranch")
    private List<Branch> childBranches;

    public Branch() {}

    public Branch(String solId, String name, String alternateBranchCode, Boolean branchAvailable) {
        this.solId = solId;
        this.name = name;
        this.alternateBranchCode = alternateBranchCode;
        this.branchAvailable = branchAvailable;
    }

    public String getSolId() {
        return solId;
    }

    public void setSolId(String solId) {
        this.solId = solId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlternateBranchCode() {
        return alternateBranchCode;
    }

    public void setAlternateBranchCode(String alternateBranchCode) {
        this.alternateBranchCode = alternateBranchCode;
    }

    public Boolean getBranchAvailable() {
        return branchAvailable;
    }

    public void setBranchAvailable(Boolean branchAvailable) {
        this.branchAvailable = branchAvailable;
    }

    public Branch getParentBranch() {
        return parentBranch;
    }

    public void setParentBranch(Branch parentBranch) {
        this.parentBranch = parentBranch;
    }

    public List<Branch> getChildBranches() {
        return childBranches;
    }

    public void setChildBranches(List<Branch> childBranches) {
        this.childBranches = childBranches;
    }
}
