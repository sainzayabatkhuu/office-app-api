package com.sol.office_app.repository;

import com.sol.office_app.entity.ReportRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRolePermissionRepository extends
        JpaRepository<ReportRolePermission, Long>,
        JpaSpecificationExecutor<ReportRolePermission> {
}
