package com.sol.office.identity.repository;

import com.sol.office.identity.domain.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {
    boolean existsByCode(String code);

    List<JobPosition> findAllByOrderByCodeAsc();
}
