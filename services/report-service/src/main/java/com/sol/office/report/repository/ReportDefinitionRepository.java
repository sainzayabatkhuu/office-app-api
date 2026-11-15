package com.sol.office.report.repository;

import com.sol.office.report.domain.ReportDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"category", "parameters"})
    Optional<ReportDefinition> findWithParametersById(Long id);

    @EntityGraph(attributePaths = {"category", "permissions"})
    Optional<ReportDefinition> findWithPermissionsById(Long id);

    /** Active reports runnable by any of the given roles at the given branch. */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select distinct r from ReportDefinition r join r.permissions p
            where r.active = true
              and p.roleCode in :roles
              and (p.branchCode is null or p.branchCode = :branch)
              and (:categoryId is null or r.category.id = :categoryId)
              and (:q is null or lower(r.title) like lower(concat('%', :q, '%'))
                   or lower(r.code) like lower(concat('%', :q, '%')))
            order by r.title
            """)
    List<ReportDefinition> findAvailable(@Param("roles") Collection<String> roles,
                                         @Param("branch") String branch,
                                         @Param("categoryId") Long categoryId,
                                         @Param("q") String q);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select r from ReportDefinition r
            where (:q is null or lower(r.title) like lower(concat('%', :q, '%'))
                   or lower(r.code) like lower(concat('%', :q, '%')))
              and (:active is null or r.active = :active)
            """)
    Page<ReportDefinition> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
