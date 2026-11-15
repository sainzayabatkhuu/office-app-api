package com.sol.office.identity.repository;

import com.sol.office.identity.domain.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Everything needed to issue a token in one query. */
    @EntityGraph(attributePaths = {"employee", "employee.branch", "roles", "roles.privileges"})
    Optional<AppUser> findByUsername(String username);

    @EntityGraph(attributePaths = {"employee", "employee.branch", "employee.department", "employee.position", "roles"})
    Optional<AppUser> findWithDetailsById(Long id);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"employee", "employee.branch", "roles"})
    @Query("""
            select u from AppUser u
            where (:q is null
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.employee.firstName) like lower(concat('%', :q, '%'))
                   or lower(u.employee.lastName) like lower(concat('%', :q, '%'))
                   or lower(u.employee.employeeCode) like lower(concat('%', :q, '%')))
              and (:branchCode is null or u.employee.branch.code = :branchCode)
              and (:enabled is null or u.enabled = :enabled)
            """)
    Page<AppUser> search(@Param("q") String q,
                         @Param("branchCode") String branchCode,
                         @Param("enabled") Boolean enabled,
                         Pageable pageable);
}
