package com.sol.office.identity.repository;

import com.sol.office.identity.domain.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"branch", "parent"})
    List<Department> findAllByOrderByCodeAsc();
}
