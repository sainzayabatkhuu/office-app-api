package com.sol.office_app.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Branch {
    @Id
    private String solId;

    private String name;
    private String alternateBranchCode;
    private Boolean branchAvailable;

    @ManyToOne
    private Branch parentBranch;

    @OneToMany(mappedBy = "parentBranch")
    private List<Branch> childBranches;

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
