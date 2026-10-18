package com.sol.office_app.service;

import com.sol.office_app.domain.request.ReportPermissionRequest;
import com.sol.office_app.dto.ReportRolePermissionDTO;
import com.sol.office_app.entity.ReportRolePermission;
import com.sol.office_app.exception.ApiException;
import com.sol.office_app.repository.BranchRepository;
import com.sol.office_app.repository.ReportRepository;
import com.sol.office_app.repository.ReportRolePermissionRepository;
import com.sol.office_app.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
public class ReportRolePermissionService {

    private static final Set<String> RUN_MODES = Set.of("V", "B");

    private final ReportRolePermissionRepository repository;
    private final ReportRepository reportRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;

    public ReportRolePermissionService(ReportRolePermissionRepository repository, ReportRepository reportRepository,
                                       RoleRepository roleRepository, BranchRepository branchRepository) {
        this.repository = repository;
        this.reportRepository = reportRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
    }

    public Page<ReportRolePermissionDTO> findDynamic(Long reportId, Pageable pageable) {
        Specification<ReportRolePermission> spec = (root, query, cb) ->
                reportId == null ? cb.conjunction() : cb.equal(root.get("report").get("id"), reportId);
        return repository.findAll(spec, pageable).map(ReportRolePermissionService::toDto);
    }

    @Transactional
    public ReportRolePermissionDTO create(ReportPermissionRequest request) {
        ReportRolePermission permission = new ReportRolePermission();
        apply(permission, request);
        return toDto(repository.save(permission));
    }

    @Transactional
    public ReportRolePermissionDTO update(Long id, ReportPermissionRequest request) {
        ReportRolePermission permission = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Report permission", id));
        apply(permission, request);
        return toDto(permission);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ApiException.notFound("Report permission", id);
        }
        repository.deleteById(id);
    }

    private void apply(ReportRolePermission permission, ReportPermissionRequest request) {
        if (request.reportId() == null || request.roleId() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "reportId and roleId are required.");
        }
        String mode = request.runInBackground() == null ? "V" : request.runInBackground().toUpperCase();
        if (!RUN_MODES.contains(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "runInBackground must be V or B.");
        }
        var report = reportRepository.findById(request.reportId())
                .orElseThrow(() -> ApiException.notFound("Report", request.reportId()));
        var role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> ApiException.notFound("Role", request.roleId()));
        var branch = request.branchId() == null || request.branchId().isBlank() ? null
                : branchRepository.findById(request.branchId())
                        .orElseThrow(() -> ApiException.notFound("Branch", request.branchId()));

        boolean duplicate = report.getRolePermissions() != null && report.getRolePermissions().stream()
                .filter(p -> !Objects.equals(p.getId(), permission.getId()))
                .anyMatch(p -> p.getRole().getId().equals(role.getId())
                        && Objects.equals(p.getBranch() == null ? null : p.getBranch().getSolId(),
                                          branch == null ? null : branch.getSolId()));
        if (duplicate) {
            throw ApiException.conflict("This role already has a permission for this report and branch.");
        }
        permission.setReport(report);
        permission.setRole(role);
        permission.setBranch(branch);
        permission.setRunInBackground(mode);
    }

    private static ReportRolePermissionDTO toDto(ReportRolePermission entity) {
        return new ReportRolePermissionDTO(
                entity.getId(),
                entity.getBranch() == null ? "" : entity.getBranch().getSolId(),
                entity.getBranch() == null ? "" : entity.getBranch().getName(),
                entity.getRole().getId(),
                entity.getRole().getName(),
                entity.getRunInBackground(),
                entity.getDelFlg());
    }
}
