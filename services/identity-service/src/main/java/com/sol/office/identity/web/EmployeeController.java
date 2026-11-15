package com.sol.office.identity.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.identity.domain.EmployeeStatus;
import com.sol.office.identity.dto.EmployeeDtos.EmployeeRequest;
import com.sol.office.identity.dto.EmployeeDtos.EmployeeResponse;
import com.sol.office.identity.dto.EmployeeDtos.TerminateRequest;
import com.sol.office.identity.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Employees")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    public PageResponse<EmployeeResponse> search(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String branchCode,
                                                 @RequestParam(required = false) Long departmentId,
                                                 @RequestParam(required = false) EmployeeStatus status,
                                                 @PageableDefault(size = 20, sort = "employeeCode", direction = Sort.Direction.ASC) Pageable pageable) {
        return employeeService.search(SecurityUtils.requireCurrentUser(), q, branchCode, departmentId, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(SecurityUtils.requireCurrentUser(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGE')")
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(SecurityUtils.requireCurrentUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGE')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(SecurityUtils.requireCurrentUser(), id, request);
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGE')")
    public EmployeeResponse terminate(@PathVariable Long id, @Valid @RequestBody TerminateRequest request) {
        return employeeService.terminate(SecurityUtils.requireCurrentUser(), id, request);
    }
}
