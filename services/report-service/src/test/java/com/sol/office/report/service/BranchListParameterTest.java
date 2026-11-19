package com.sol.office.report.service;

import com.sol.office.common.security.CurrentUser;
import com.sol.office.report.domain.ReportDefinition;
import com.sol.office.report.domain.ReportParameter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** How {@code com.sol.jasper.params.BranchList} parameters are filled when a report is run. */
class BranchListParameterTest {

    private final ReportExecutionService service =
            new ReportExecutionService(null, null, null, null, null, null, null, null);

    private static ReportDefinition reportWithBranches() {
        ReportDefinition report = new ReportDefinition();
        report.setTitle("Branch report");
        ReportParameter p = new ReportParameter();
        p.setName("branches");
        p.setLabel("Branches");
        p.setJavaType("com.sol.jasper.params.BranchList");
        p.setInputType("BranchList");
        report.getParameters().add(p);
        return report;
    }

    private static CurrentUser user(boolean multiBranch) {
        return new CurrentUser(1L, "u", "User", "101", multiBranch, Set.of(), Set.of(), "t");
    }

    @Test
    void multiBranchUserChoosesBranches() {
        assertThat(service.resolveParameters(reportWithBranches(), user(true), Map.of("branches", "101,205")))
                .containsEntry("branches", "101,205");
    }

    @Test
    void emptyChoiceMeansAllBranches() {
        assertThat(service.resolveParameters(reportWithBranches(), user(true), Map.of()))
                .containsEntry("branches", "ALL");
    }

    @Test
    void singleBranchUserIsLockedToOwnBranch() {
        assertThat(service.resolveParameters(reportWithBranches(), user(false), Map.of("branches", "205,999")))
                .containsEntry("branches", "101");
        assertThat(service.resolveParameters(reportWithBranches(), user(false), Map.of()))
                .containsEntry("branches", "101");
    }
}
