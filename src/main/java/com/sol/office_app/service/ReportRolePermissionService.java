package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.ReportRolePermissionDTO;
import com.sol.office_app.entity.ReportRolePermission;
import com.sol.office_app.repository.ReportRolePermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReportRolePermissionService implements GeneralService<ReportRolePermissionDTO, Long> {

    public final ReportRolePermissionRepository repository;

    public ReportRolePermissionService(ReportRolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<ReportRolePermissionDTO> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<ReportRolePermissionDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<ReportRolePermissionDTO> get(Long aLong) {
        return Optional.empty();
    }

    @Override
    public Optional<ReportRolePermissionDTO> save(ReportRolePermissionDTO entity) {
        return Optional.empty();
    }

    @Override
    public Optional<ReportRolePermissionDTO> update(Long aLong, ReportRolePermissionDTO entity) {
        return Optional.empty();
    }

    @Override
    public void delete(Long aLong) {

    }

    public Page<ReportRolePermissionDTO> findDynamic(Long reportId, Pageable pageable) {
        Specification<ReportRolePermission> spec = null;

        if (reportId != null) {
            Specification<ReportRolePermission> typeSpec = (root, query, cb) ->
                    cb.equal(root.get("paramType"), reportId);
            spec = spec == null ? typeSpec : spec.and(typeSpec);
        }

        return repository.findAll(spec, pageable).map(entity -> new ReportRolePermissionDTO(
                entity.getId(),
                entity.getBranch().equals(null) ? "": entity.getBranch().getSolId(),
                entity.getBranch().equals(null) ? "": entity.getBranch().getName(),
                entity.getRole().getId(),
                entity.getRole().getName(),
                entity.getRunInBackground(),
                entity.getDelFlg()
        ));
    }
}
