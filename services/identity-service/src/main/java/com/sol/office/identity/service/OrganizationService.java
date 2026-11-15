package com.sol.office.identity.service;

import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.identity.domain.Branch;
import com.sol.office.identity.domain.Department;
import com.sol.office.identity.domain.JobPosition;
import com.sol.office.identity.dto.OrgDtos.BranchRequest;
import com.sol.office.identity.dto.OrgDtos.BranchResponse;
import com.sol.office.identity.dto.OrgDtos.DepartmentRequest;
import com.sol.office.identity.dto.OrgDtos.DepartmentResponse;
import com.sol.office.identity.dto.OrgDtos.PositionRequest;
import com.sol.office.identity.dto.OrgDtos.PositionResponse;
import com.sol.office.identity.mapper.IdentityMapper;
import com.sol.office.identity.repository.BranchRepository;
import com.sol.office.identity.repository.DepartmentRepository;
import com.sol.office.identity.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Branches, departments and job positions. Records are deactivated rather than deleted because employees
 * and historical reports keep referring to them.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository positionRepository;
    private final IdentityMapper mapper;

    // ---- branches

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches() {
        return branchRepository.findAllByOrderByCodeAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByCode(request.code())) {
            throw new ConflictException("Branch code already exists: " + request.code());
        }
        Branch branch = new Branch();
        applyBranch(branch, request);
        return mapper.toResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = branchRepository.findById(id).orElseThrow(() -> NotFoundException.of("Branch", id));
        if (!branch.getCode().equals(request.code()) && branchRepository.existsByCode(request.code())) {
            throw new ConflictException("Branch code already exists: " + request.code());
        }
        if (Objects.equals(request.parentId(), id)) {
            throw new IllegalArgumentException("A branch cannot be its own parent");
        }
        applyBranch(branch, request);
        return mapper.toResponse(branch);
    }

    private void applyBranch(Branch branch, BranchRequest request) {
        branch.setCode(request.code());
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        branch.setParent(request.parentId() == null ? null : getBranch(request.parentId()));
        if (request.active() != null) branch.setActive(request.active());
    }

    Branch getBranch(Long id) {
        return branchRepository.findById(id).orElseThrow(() -> NotFoundException.of("Branch", id));
    }

    // ---- departments

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAllByOrderByCodeAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByCode(request.code())) {
            throw new ConflictException("Department code already exists: " + request.code());
        }
        Department department = new Department();
        applyDepartment(department, request);
        return mapper.toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = getDepartment(id);
        if (!department.getCode().equals(request.code()) && departmentRepository.existsByCode(request.code())) {
            throw new ConflictException("Department code already exists: " + request.code());
        }
        if (Objects.equals(request.parentId(), id)) {
            throw new IllegalArgumentException("A department cannot be its own parent");
        }
        applyDepartment(department, request);
        return mapper.toResponse(department);
    }

    private void applyDepartment(Department department, DepartmentRequest request) {
        department.setCode(request.code());
        department.setName(request.name());
        department.setBranch(request.branchId() == null ? null : getBranch(request.branchId()));
        department.setParent(request.parentId() == null ? null : getDepartment(request.parentId()));
        if (request.active() != null) department.setActive(request.active());
    }

    Department getDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Department", id));
    }

    // ---- positions

    @Transactional(readOnly = true)
    public List<PositionResponse> listPositions() {
        return positionRepository.findAllByOrderByCodeAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public PositionResponse createPosition(PositionRequest request) {
        if (positionRepository.existsByCode(request.code())) {
            throw new ConflictException("Position code already exists: " + request.code());
        }
        JobPosition position = new JobPosition();
        applyPosition(position, request);
        return mapper.toResponse(positionRepository.save(position));
    }

    @Transactional
    public PositionResponse updatePosition(Long id, PositionRequest request) {
        JobPosition position = getPosition(id);
        if (!position.getCode().equals(request.code()) && positionRepository.existsByCode(request.code())) {
            throw new ConflictException("Position code already exists: " + request.code());
        }
        applyPosition(position, request);
        return mapper.toResponse(position);
    }

    private void applyPosition(JobPosition position, PositionRequest request) {
        position.setCode(request.code());
        position.setName(request.name());
        position.setDescription(request.description());
        if (request.active() != null) position.setActive(request.active());
    }

    JobPosition getPosition(Long id) {
        return positionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Position", id));
    }
}
