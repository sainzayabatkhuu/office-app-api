package com.sol.office_app.service;

import com.sol.office_app.config.PrivilegeRules;
import com.sol.office_app.dto.ExportSummaryDTO;
import com.sol.office_app.exception.ApiException;
import org.springframework.core.io.PathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Map;

import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.dto.ExportMonitorDTO;
import com.sol.office_app.domain.response.NotificationMessage;
import com.sol.office_app.entity.ExportMonitor;
import com.sol.office_app.repository.ExportMonitorRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ExportMonitorService {

    private final ExportMonitorRepository repository;

    public ExportMonitorService(ExportMonitorRepository repository) {
        this.repository = repository;
    }


    public NotificationMessage exportDataToExcel(
            List<String[]> data,
            Authentication authentication,
            String functionId) {
        CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();
        String refNo = "REF-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        ExportMonitor monitor = new ExportMonitor();
        monitor.setReferenceNumber(refNo);
        monitor.setUserId(user.getName());
        monitor.setFunctionId(functionId);
        monitor.setRequestedTime(now);
        monitor.setExportStatus("PENDING");
        repository.save(monitor);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            // Create Excel rows
            for (int rowNum = 0; rowNum < data.size(); rowNum++) {
                Row row = sheet.createRow(rowNum);
                String[] rowData = data.get(rowNum);
                for (int i = 0; i < rowData.length; i++) {
                    row.createCell(i).setCellValue(rowData[i] != null ? rowData[i] : "");
                }
            }

            // Create directory structure
            Path folder = Path.of("exports", refNo);
            Files.createDirectories(folder);

            String fileName = "export_" + refNo + ".xlsx";
            Path filePath = folder.resolve(fileName);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }

            // Update monitor after successful export
            monitor.setFileName(fileName);
            monitor.setExportStatus("COMPLETED");
            monitor.setResponseTime(LocalDateTime.now());
            int records = Math.max(0, data.size() - 1); // first row is the header
            monitor.setTotalRecords(records);
            monitor.setFetchSize(records);
            repository.save(monitor);

            return new NotificationMessage("success","Export Data",
                    "Export request submitted successfully! Reference No: " + refNo,
                    Map.of("exportId", String.valueOf(monitor.getId()), "referenceNumber", refNo));

        } catch (IOException ioEx) {
            monitor.setExportStatus("UNCREATED");
            monitor.setResponseTime(LocalDateTime.now());
            repository.save(monitor);
            throw new RuntimeException("Failed to write Excel file: " + ioEx.getMessage(), ioEx);

        } catch (Exception ex) {
            monitor.setExportStatus("UNCREATED");
            monitor.setResponseTime(LocalDateTime.now());
            repository.save(monitor);
            throw new RuntimeException("Export request failed: " + ex.getMessage(), ex);
        }
    }

    /** Filters are optional; text filters are case-insensitive "contains", dates are inclusive (yyyy-MM-dd). */
    public Page<ExportMonitorDTO> findDynamic(
            String referenceNumber,
            String username,
            String functionId,
            String status,
            String startDate,
            String endDate,
            Pageable pageable) {
        Specification<ExportMonitor> spec = (root, query, cb) -> cb.conjunction();
        if (hasText(referenceNumber)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("referenceNumber")), like(referenceNumber)));
        }
        if (hasText(username)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("userId")), like(username)));
        }
        if (hasText(functionId)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("functionId")), like(functionId)));
        }
        if (hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("exportStatus")), status.trim().toUpperCase()));
        }
        if (hasText(startDate)) {
            LocalDateTime from = day(startDate).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedTime"), from));
        }
        if (hasText(endDate)) {
            LocalDateTime to = day(endDate).plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("requestedTime"), to));
        }
        return repository.findAll(spec, pageable).map(ExportMonitorService::toDto);
    }

    /** Activity between two dates (inclusive); defaults to the last 30 days. */
    public ExportSummaryDTO summary(String startDate, String endDate) {
        LocalDate end = hasText(endDate) ? day(endDate) : LocalDate.now();
        LocalDate start = hasText(startDate) ? day(startDate) : end.minusDays(29);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        List<ExportSummaryDTO.Bucket> byStatus = buckets(repository.countByStatus(from, to));
        return new ExportSummaryDTO(
                byStatus.stream().mapToLong(ExportSummaryDTO.Bucket::exports).sum(),
                byStatus.stream().mapToLong(ExportSummaryDTO.Bucket::records).sum(),
                byStatus,
                buckets(repository.countByFunction(from, to)),
                buckets(repository.countByUser(from, to)));
    }

    /** The generated file; for the user who exported it or anyone who may monitor exports. */
    public ReportHistoryService.Download download(Long id, CustomUserPrincipal caller) {
        ExportMonitor monitor = repository.findById(id).orElseThrow(() -> ApiException.notFound("Export", id));
        if (!caller.getName().equals(monitor.getUserId()) && !PrivilegeRules.allows(caller, "get", "/export-monitor")) {
            throw ApiException.notFound("Export", id);
        }
        if (!"COMPLETED".equalsIgnoreCase(monitor.getExportStatus()) || monitor.getFileName() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "OUTPUT_NOT_AVAILABLE", "The export file is not available.");
        }
        Path base = Path.of("exports").toAbsolutePath().normalize();
        Path file = base.resolve(monitor.getReferenceNumber()).resolve(monitor.getFileName()).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            throw new ApiException(HttpStatus.GONE, "OUTPUT_DELETED", "The export file no longer exists.");
        }
        return new ReportHistoryService.Download(new PathResource(file), monitor.getFileName(),
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private static List<ExportSummaryDTO.Bucket> buckets(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new ExportSummaryDTO.Bucket(r[0] == null ? "—" : String.valueOf(r[0]),
                        ((Number) r[1]).longValue(), ((Number) r[2]).longValue()))
                .sorted(Comparator.comparingLong(ExportSummaryDTO.Bucket::exports).reversed())
                .limit(20)
                .toList();
    }

    private static ExportMonitorDTO toDto(ExportMonitor entity) {
        return new ExportMonitorDTO(
                entity.getId(),
                entity.getReferenceNumber(),
                entity.getUserId(),
                entity.getFunctionId(),
                entity.getFetchSize(),
                entity.getRequestedTime(),
                entity.getResponseTime(),
                entity.getExportStatus(),
                entity.getFileName(),
                entity.getTotalRecords());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }

    private static LocalDate day(String value) {
        try {
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("INVALID_PARAMETER", "Dates must be yyyy-MM-dd");
        }
    }
}
