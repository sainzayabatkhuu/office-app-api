package com.sol.office.identity.repository;

import com.sol.office.identity.domain.Employee;
import com.sol.office.identity.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByRegisterNo(String registerNo);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"branch", "department", "position", "manager"})
    Optional<Employee> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"branch", "department", "position"})
    @Query("""
            select e from Employee e
            where (:q is null
                   or lower(e.employeeCode) like lower(concat('%', :q, '%'))
                   or lower(e.firstName) like lower(concat('%', :q, '%'))
                   or lower(e.lastName) like lower(concat('%', :q, '%'))
                   or lower(e.registerNo) like lower(concat('%', :q, '%'))
                   or lower(e.email) like lower(concat('%', :q, '%')))
              and (:branchCode is null or e.branch.code = :branchCode)
              and (:departmentId is null or e.department.id = :departmentId)
              and (:status is null or e.status = :status)
            """)
    Page<Employee> search(@Param("q") String q,
                          @Param("branchCode") String branchCode,
                          @Param("departmentId") Long departmentId,
                          @Param("status") EmployeeStatus status,
                          Pageable pageable);
}
