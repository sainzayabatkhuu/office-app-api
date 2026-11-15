package com.sol.office.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

/**
 * Grants a role (optionally only at one branch) the right to run a report.
 */
@Getter
@Setter
@Entity
@Table(name = "report_permission")
public class ReportPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private ReportDefinition report;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    /** Null = every branch. */
    @Column(name = "branch_code", length = 10)
    private String branchCode;

    /** Overrides the report's execution mode for this role, e.g. force ASYNC for heavy users. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "execution_mode", length = 10)
    private ExecutionMode executionMode;

    public boolean appliesTo(Set<String> roles, String userBranch) {
        return roles.contains(roleCode) && (branchCode == null || branchCode.equals(userBranch));
    }
}
