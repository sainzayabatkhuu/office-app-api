package com.sol.office.report.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.exception.OfficeException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.report.domain.ExportMonitor;
import com.sol.office.report.dto.ExportDtos.Bucket;
import com.sol.office.report.dto.ExportDtos.ExportMonitorResponse;
import com.sol.office.report.dto.ExportDtos.ExportResult;
import com.sol.office.report.dto.ExportDtos.ExportSummaryResponse;
import com.sol.office.report.repository.ExportMonitorRepository;
import com.sol.office.report.service.ReportExecutionService.Download;
import com.sol.office.report.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Excel exports of frontend grids and their monitoring (CSDXLMNT, CSSXLMNT), ported from office-app-api.
 * Any signed-in user may export; seeing everyone's exports needs {@value #MONITOR}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportMonitorService {

    public static final String MONITOR = "EXPORT_MONITOR";
    private static final int MAX_ROWS = 100_000;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportMonitorRepository repository;
    private final FileStorage storage;
    private final Clock clock;

    /** Writes the rows (first row = header) to a workbook and records the export. */
    public ExportResult export(CurrentUser user, String functionId, List<String[]> rows) {
        if (functionId == null || functionId.isBlank() || functionId.length() > 20) {
            throw new BusinessException("VALIDATION_FAILED", "functionId is required (max 20 characters).");
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("VALIDATION_FAILED", "Nothing to export.");
        }
        if (rows.size() > MAX_ROWS + 1) {
            throw new BusinessException("EXPORT_TOO_LARGE", "At most " + MAX_ROWS + " rows can be exported at once.");
        }
        String reference = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ExportMonitor monitor = new ExportMonitor();
        monitor.setReferenceNumber(reference);
        monitor.setUserId(user.username());
        monitor.setFunctionId(functionId.trim().toUpperCase());
        monitor.setRequestedTime(LocalDateTime.now(clock));
        monitor.setExportStatus(ExportMonitor.PENDING);
        monitor = repository.save(monitor);

        String fileName = "export_" + reference + ".xlsx";
        String key = "exports/" + reference + "/" + fileName;
        try {
            storage.store(key, new ByteArrayInputStream(workbook(rows)));
        } catch (IOException | RuntimeException e) {
            log.error("Excel export {} failed", reference, e);
            monitor.setExportStatus(ExportMonitor.UNCREATED);
            monitor.setResponseTime(LocalDateTime.now(clock));
            repository.save(monitor);
            throw new OfficeException(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_FAILED",
                    "The export could not be created. Reference No: " + reference);
        }
        int records = rows.size() - 1;
        monitor.setFileName(fileName);
        monitor.setStorageKey(key);
        monitor.setExportStatus(ExportMonitor.COMPLETED);
        monitor.setResponseTime(LocalDateTime.now(clock));
        monitor.setTotalRecords(records);
        monitor.setFetchSize(records);
        monitor = repository.save(monitor);
        return new ExportResult("success", "Export Data",
                "Export request submitted successfully! Reference No: " + reference,
                Map.of("exportId", String.valueOf(monitor.getId()), "referenceNumber", reference));
    }

    /** Filters optional; text filters are case-insensitive "contains", dates inclusive (yyyy-MM-dd). */
    @Transactional(readOnly = true)
    public PageResponse<ExportMonitorResponse> search(String referenceNumber, String username, String functionId,
                                                      String status, String startDate, String endDate, Pageable pageable) {
        Specification<ExportMonitor> spec = (root, q, cb) -> cb.conjunction();
        if (hasText(referenceNumber)) {
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("referenceNumber")), like(referenceNumber)));
        }
        if (hasText(username)) {
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("userId")), like(username)));
        }
        if (hasText(functionId)) {
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("functionId")), like(functionId)));
        }
        if (hasText(status)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("exportStatus"), status.trim().toUpperCase()));
        }
        if (hasText(startDate)) {
            LocalDateTime from = day(startDate).atStartOfDay();
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("requestedTime"), from));
        }
        if (hasText(endDate)) {
            LocalDateTime to = day(endDate).plusDays(1).atStartOfDay();
            spec = spec.and((root, q, cb) -> cb.lessThan(root.get("requestedTime"), to));
        }
        return PageResponse.of(repository.findAll(spec, pageable), ExportMonitorService::toResponse);
    }

    /** Activity between two dates (inclusive); defaults to the last 30 days. */
    @Transactional(readOnly = true)
    public ExportSummaryResponse summary(String startDate, String endDate) {
        LocalDate end = hasText(endDate) ? day(endDate) : LocalDate.now(clock);
        LocalDate start = hasText(startDate) ? day(startDate) : end.minusDays(29);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        List<Bucket> byStatus = buckets(repository.countByStatus(from, to));
        return new ExportSummaryResponse(
                byStatus.stream().mapToLong(Bucket::exports).sum(),
                byStatus.stream().mapToLong(Bucket::records).sum(),
                byStatus,
                buckets(repository.countByFunction(from, to)),
                buckets(repository.countByUser(from, to)));
    }

    /** The workbook; for the user who exported it, or anyone with {@value #MONITOR}. */
    @Transactional(readOnly = true)
    public Download download(CurrentUser user, Long id) {
        ExportMonitor monitor = repository.findById(id).orElseThrow(() -> NotFoundException.of("Export", id));
        if (!user.username().equals(monitor.getUserId()) && !user.hasAuthority(MONITOR)) {
            // same answer as a missing id: don't reveal other users' exports
            throw NotFoundException.of("Export", id);
        }
        if (!ExportMonitor.COMPLETED.equals(monitor.getExportStatus()) || monitor.getStorageKey() == null) {
            throw new OfficeException(HttpStatus.CONFLICT, "OUTPUT_NOT_AVAILABLE", "The export file is not available.");
        }
        if (!storage.exists(monitor.getStorageKey())) {
            throw new OfficeException(HttpStatus.GONE, "OUTPUT_DELETED", "The export file no longer exists.");
        }
        return new Download(storage.load(monitor.getStorageKey()), monitor.getFileName(), XLSX);
    }

    private static byte[] workbook(List<String[]> rows) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(500); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                String[] values = rows.get(r);
                if (values == null) {
                    continue;
                }
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c] == null ? "" : values[c]);
                }
            }
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
    }

    private static List<Bucket> buckets(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new Bucket(r[0] == null ? "—" : String.valueOf(r[0]),
                        ((Number) r[1]).longValue(), ((Number) r[2]).longValue()))
                .sorted(Comparator.comparingLong(Bucket::exports).reversed())
                .limit(20)
                .toList();
    }

    private static ExportMonitorResponse toResponse(ExportMonitor e) {
        return new ExportMonitorResponse(e.getId(), e.getReferenceNumber(), e.getUserId(), e.getFunctionId(),
                e.getFetchSize(), e.getRequestedTime(), e.getResponseTime(), e.getExportStatus(), e.getFileName(),
                e.getTotalRecords());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }

    private static LocalDate day(String value) {
        String v = value.trim();
        try {
            return LocalDate.parse(v.substring(0, Math.min(10, v.length())));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Dates must be yyyy-MM-dd");
        }
    }
}
