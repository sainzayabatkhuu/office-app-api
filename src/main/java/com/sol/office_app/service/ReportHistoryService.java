package com.sol.office_app.service;

import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.config.PrivilegeRules;
import com.sol.office_app.dto.ReportHistoryDto;
import com.sol.office_app.entity.ReportHistory;
import com.sol.office_app.enums.ReportStatus;
import com.sol.office_app.exception.ApiException;
import com.sol.office_app.mapper.ReportHistoryMapper;
import com.sol.office_app.repository.ReportHistoryRepository;
import com.sol.office_app.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class ReportHistoryService {

    private final ReportHistoryRepository repository;
    private final String outputDir;

    public ReportHistoryService(ReportHistoryRepository repository, @Value("${report.output.dir}") String outputDir) {
        this.repository = repository;
        this.outputDir = outputDir;
    }

    public record Download(Resource resource, String fileName, MediaType mediaType) {
    }

    /** {@code startDate}/{@code endDate} are inclusive dates (yyyy-MM-dd) on the start time. */
    public Page<ReportHistoryDto> findDynamic(
            String executer,
            String branch,
            String startDate,
            String endDate,
            Pageable pageable) {
        Specification<ReportHistory> spec = (root, query, cb) -> cb.conjunction();

        if (executer != null && !executer.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("executedBy"), executer));
        }
        if (branch != null && !branch.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("branchCode")), "%" + branch.toLowerCase() + "%"));
        }
        if (startDate != null && !startDate.isBlank()) {
            LocalDate from = parseDate("startDate", startDate);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startedAt"), from.atStartOfDay()));
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDate to = parseDate("endDate", endDate);
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("startedAt"), to.plusDays(1).atStartOfDay()));
        }

        return repository.findAll(spec, pageable).map(ReportHistoryMapper::toDto);
    }

    /**
     * The generated file of a successful run. Allowed for the user who ran it, or for anyone who may
     * view report histories.
     */
    public Download download(Long id, CustomUserPrincipal caller) {
        ReportHistory history = repository.findById(id).orElseThrow(() -> ApiException.notFound("Report history", id));
        boolean owner = caller.getName().equals(history.getExecutedBy());
        if (!owner && !PrivilegeRules.allows(caller, "get", "/report-histories")) {
            // Same answer as a missing id: don't reveal that someone else's run exists.
            throw ApiException.notFound("Report history", id);
        }
        if (history.getStatus() != ReportStatus.SUCCESS || history.getReportOutputFile() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "OUTPUT_NOT_AVAILABLE",
                    "Report output is not available (status " + history.getStatus() + ").");
        }
        Path base = Paths.get(outputDir).toAbsolutePath().normalize();
        Path file = base.resolve(history.getExecutedBy()).resolve(history.getReportOutputFile()).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            throw new ApiException(HttpStatus.GONE, "OUTPUT_DELETED", "The report file no longer exists.");
        }
        String name = history.getReportOutputFile();
        String extension = name.substring(name.lastIndexOf('.') + 1);
        return new Download(new PathResource(file), name, Utils.getMediaTypeByFormat(extension));
    }

    private static LocalDate parseDate(String name, String value) {
        try {
            return LocalDate.parse(value.substring(0, Math.min(10, value.length())));
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("INVALID_PARAMETER", name + " must be yyyy-MM-dd");
        }
    }
}
