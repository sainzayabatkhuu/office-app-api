package com.sol.office_app.service;

import com.sol.office_app.dto.ReportHistoryDto;
import com.sol.office_app.entity.CoreParameter;
import com.sol.office_app.entity.ReportHistory;
import com.sol.office_app.mapper.ReportHistoryMapper;
import com.sol.office_app.repository.ReportHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ReportHistoryService {

    private final ReportHistoryRepository repository;

    public ReportHistoryService(ReportHistoryRepository repository) {
        this.repository = repository;
    }

    public Page<ReportHistoryDto> findDynamic(
            String executer,
            String branch,
            String startDate,
            String endDate,
            Pageable pageable) {
        Specification<ReportHistory> spec = null;
        System.out.println(executer);
        System.out.println(branch);
        System.out.println(startDate);
        System.out.println(endDate);

        if (executer != null && !executer.isBlank()) {
            Specification<ReportHistory> executeSpec = (root, query, cb) ->
                    cb.equal(root.get("executedBy"), executer);
            spec = spec == null ? executeSpec : spec.and(executeSpec);
        }

        if (branch != null && !branch.isBlank()) {
            Specification<ReportHistory> branchSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("branchCode")), "%" + branch.toLowerCase() + "%");
            spec = spec == null ? branchSpec : spec.and(branchSpec);
        }
//
//        if (startDate != null && !startDate.isBlank()) {
//            Specification<ReportHistory> nameSpec = (root, query, cb) ->
//                    cb.like(cb.lower(root.get("paramName")), "%" + paramName.toLowerCase() + "%");
//            spec = spec == null ? nameSpec : spec.and(nameSpec);
//        }
//
//        if (endDate != null && !endDate.isBlank()) {
//            Specification<ReportHistory> codeSpec = (root, query, cb) ->
//                    cb.like(cb.lower(root.get("paramCode")), "%" + paramCode.toLowerCase() + "%");
//            spec = spec == null ? codeSpec : spec.and(codeSpec);
//        }

        return repository.findAll(spec, pageable).map(ReportHistoryMapper::toDto);
    }

}
