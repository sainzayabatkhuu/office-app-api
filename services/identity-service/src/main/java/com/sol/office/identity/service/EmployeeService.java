package com.sol.office.identity.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.identity.domain.Employee;
import com.sol.office.identity.domain.EmployeeStatus;
import com.sol.office.identity.dto.EmployeeDtos.EmployeeRequest;
import com.sol.office.identity.dto.EmployeeDtos.EmployeeResponse;
import com.sol.office.identity.dto.EmployeeDtos.TerminateRequest;
import com.sol.office.identity.mapper.IdentityMapper;
import com.sol.office.identity.repository.AppUserRepository;
import com.sol.office.identity.repository.EmployeeRepository;
import com.sol.office.identity.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationService organizationService;
    private final IdentityMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(CurrentUser user, String q, String branchCode, Long departmentId,
                                                 EmployeeStatus status, Pageable pageable) {
        String branch = BranchScope.effectiveBranch(user, emptyToNull(branchCode));
        return PageResponse.of(employeeRepository.search(emptyToNull(q), branch, departmentId, status, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(CurrentUser user, Long id) {
        Employee employee = load(id);
        BranchScope.check(user, employee.getBranch().getCode());
        return mapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse create(CurrentUser user, EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new ConflictException("Employee code already exists: " + request.employeeCode());
        }
        if (StringUtils.hasText(request.registerNo()) && employeeRepository.existsByRegisterNo(request.registerNo())) {
            throw new ConflictException("Register number already exists: " + request.registerNo());
        }
        if (StringUtils.hasText(request.email()) && employeeRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }
        Employee employee = new Employee();
        apply(employee, request);
        BranchScope.check(user, employee.getBranch().getCode());
        return mapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(CurrentUser user, Long id, EmployeeRequest request) {
        Employee employee = load(id);
        BranchScope.check(user, employee.getBranch().getCode());
        if (request.version() == null || !request.version().equals(employee.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Employee.class, id);
        }
        if (!employee.getEmployeeCode().equals(request.employeeCode())
                && employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new ConflictException("Employee code already exists: " + request.employeeCode());
        }
        if (Objects.equals(request.managerId(), id)) {
            throw new IllegalArgumentException("An employee cannot be their own manager");
        }
        if (request.status() == EmployeeStatus.TERMINATED && employee.getStatus() != EmployeeStatus.TERMINATED) {
            throw new BusinessException("USE_TERMINATE", "Use the terminate action to terminate an employee");
        }
        apply(employee, request);
        BranchScope.check(user, employee.getBranch().getCode());
        return mapper.toResponse(employee);
    }

    /** Ends employment and immediately revokes the employee's sign-in, if any. */
    @Transactional
    public EmployeeResponse terminate(CurrentUser user, Long id, TerminateRequest request) {
        Employee employee = load(id);
        BranchScope.check(user, employee.getBranch().getCode());
        if (request.terminationDate().isBefore(employee.getHireDate())) {
            throw new BusinessException("INVALID_DATE", "Termination date is before the hire date");
        }
        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setTerminationDate(request.terminationDate());
        userRepository.findById(id).ifPresent(account -> {
            account.setEnabled(false);
            refreshTokenRepository.revokeAllForUser(account.getId(), clock.instant());
        });
        return mapper.toResponse(employee);
    }

    private void apply(Employee e, EmployeeRequest r) {
        e.setEmployeeCode(r.employeeCode());
        e.setLastName(r.lastName());
        e.setFirstName(r.firstName());
        e.setRegisterNo(emptyToNull(r.registerNo()));
        e.setGender(r.gender());
        e.setBirthDate(r.birthDate());
        e.setEmail(emptyToNull(r.email()));
        e.setPhone(r.phone());
        e.setHireDate(r.hireDate());
        if (r.status() != null) e.setStatus(r.status());
        e.setBranch(organizationService.getBranch(r.branchId()));
        e.setDepartment(r.departmentId() == null ? null : organizationService.getDepartment(r.departmentId()));
        e.setPosition(r.positionId() == null ? null : organizationService.getPosition(r.positionId()));
        e.setManager(r.managerId() == null ? null : load(r.managerId()));
    }

    private Employee load(Long id) {
        return employeeRepository.findWithDetailsById(id).orElseThrow(() -> NotFoundException.of("Employee", id));
    }

    private static String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
