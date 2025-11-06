package com.sol.office_app.service;

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
            monitor.setTotalRecords(data.size());
            repository.save(monitor);

            return new NotificationMessage("success","Export Data",
                    "Export request submitted successfully! Reference No: " + refNo, null
                    );

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

    public Page<ExportMonitorDTO> findDynamic(
            String referenceNumber,
            String username,
            String fetchSize,
            String functionId,
            String requestedTime,
            String status,
            PageRequest pageable) {
        Specification<ExportMonitor> spec = null;

        if (referenceNumber != null && !referenceNumber.isBlank()) {
            Specification<ExportMonitor> typeSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("referenceNumber")), "%" + referenceNumber + "%");
            spec = spec == null ? typeSpec : spec.and(typeSpec);
        }

        if (username != null && !username.isBlank()) {
            Specification<ExportMonitor> codeSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("userId")), "%" + username + "%");
            spec = spec == null ? codeSpec : spec.and(codeSpec);
        }

        if (fetchSize != null && !fetchSize.isBlank()) {
            Specification<ExportMonitor> nameSpec = (root, query, cb) ->
                    cb.equal(root.get("fetchSize"), fetchSize);
            spec = spec == null ? nameSpec : spec.and(nameSpec);
        }

        if (functionId != null && !functionId.isBlank()) {
            Specification<ExportMonitor> nameSpec = (root, query, cb) ->
                    cb.equal(cb.lower(root.get("functionId")), functionId);
            spec = spec == null ? nameSpec : spec.and(nameSpec);
        }

        if (requestedTime != null && !requestedTime.isBlank()) {
            Specification<ExportMonitor> nameSpec = (root, query, cb) ->
                    cb.equal(root.get("requestedTime"), requestedTime);
            spec = spec == null ? nameSpec : spec.and(nameSpec);
        }

        if (status != null && !status.isBlank()) {
            Specification<ExportMonitor> nameSpec = (root, query, cb) ->
                    cb.equal(cb.lower(root.get("exportStatus")), status);
            spec = spec == null ? nameSpec : spec.and(nameSpec);
        }

        return repository.findAll(spec, pageable)
                .map(entity -> new ExportMonitorDTO(
                entity.getId(),
                entity.getReferenceNumber(),
                entity.getFunctionId(),
                entity.getUserId(),
                entity.getFetchSize(),
                entity.getRequestedTime(),
                entity.getResponseTime(),
                entity.getExportStatus(),
                entity.getFileName(),
                entity.getTotalRecords()
        ));
    }
}
