package com.sol.office_app.repository;

import com.sol.office_app.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>{

    @Query("""
    SELECT r FROM Report r
    JOIN r.rolePermissions rp
    WHERE rp.role.id IN :roleIds
""")
    Page<Report> findReportsByUserRoleIds(@Param("roleIds") List<Long> roleIds, Pageable pageable);
}
