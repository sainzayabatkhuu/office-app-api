package com.sol.office.identity.web;

import com.sol.office.identity.dto.OrgDtos.BranchRequest;
import com.sol.office.identity.dto.OrgDtos.BranchResponse;
import com.sol.office.identity.dto.OrgDtos.DepartmentRequest;
import com.sol.office.identity.dto.OrgDtos.DepartmentResponse;
import com.sol.office.identity.dto.OrgDtos.PositionRequest;
import com.sol.office.identity.dto.OrgDtos.PositionResponse;
import com.sol.office.identity.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reference lists are readable by any signed-in user (drop-downs); changes need ORG_MANAGE.
 */
@Tag(name = "Organization")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/branches")
    public List<BranchResponse> branches() {
        return organizationService.listBranches();
    }

    @PostMapping("/branches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public BranchResponse createBranch(@Valid @RequestBody BranchRequest request) {
        return organizationService.createBranch(request);
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public BranchResponse updateBranch(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return organizationService.updateBranch(id, request);
    }

    @GetMapping("/departments")
    public List<DepartmentResponse> departments() {
        return organizationService.listDepartments();
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return organizationService.createDepartment(request);
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public DepartmentResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return organizationService.updateDepartment(id, request);
    }

    @GetMapping("/positions")
    public List<PositionResponse> positions() {
        return organizationService.listPositions();
    }

    @PostMapping("/positions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public PositionResponse createPosition(@Valid @RequestBody PositionRequest request) {
        return organizationService.createPosition(request);
    }

    @PutMapping("/positions/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public PositionResponse updatePosition(@PathVariable Long id, @Valid @RequestBody PositionRequest request) {
        return organizationService.updatePosition(id, request);
    }
}
