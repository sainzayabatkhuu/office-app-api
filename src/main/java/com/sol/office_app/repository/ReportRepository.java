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

    @Query(value = """
    SELECT DISTINCT r FROM Report r
    INNER JOIN r.rolePermissions rp
    WHERE rp.role.name IN :roleNames
      AND (:title = '' OR lower(r.title) LIKE lower(concat('%', :title, '%')))
      AND (:fileName = '' OR lower(r.fileName) LIKE lower(concat('%', :fileName, '%')))
""", countQuery = """
    SELECT count(DISTINCT r) FROM Report r
    INNER JOIN r.rolePermissions rp
    WHERE rp.role.name IN :roleNames
      AND (:title = '' OR lower(r.title) LIKE lower(concat('%', :title, '%')))
      AND (:fileName = '' OR lower(r.fileName) LIKE lower(concat('%', :fileName, '%')))
""")
    Page<Report> findReportsByUserRoleNames(@Param("roleNames") List<String> roleNames,
                                            @Param("title") String title,
                                            @Param("fileName") String fileName,
                                            Pageable pageable);

    @Query(value = """
    SELECT DISTINCT r FROM Report r
    JOIN r.rolePermissions rp
    WHERE rp.role.id IN :roleIds
""", countQuery = """
    SELECT count(DISTINCT r) FROM Report r
    JOIN r.rolePermissions rp
    WHERE rp.role.id IN :roleIds
""")
    Page<Report> findReportsByUserRoleIds(@Param("roleIds") List<Long> roleIds, Pageable pageable);

    @Query("""
    SELECT r FROM Report r
    WHERE r.fileName = :fileName
""")
    Report finnByFileName(@Param("fileName") String filename);


}
