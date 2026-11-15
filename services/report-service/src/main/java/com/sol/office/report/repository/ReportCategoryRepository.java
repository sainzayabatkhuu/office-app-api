package com.sol.office.report.repository;

import com.sol.office.report.domain.ReportCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCategoryRepository extends JpaRepository<ReportCategory, Long> {
    List<ReportCategory> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByCode(String code);
}
