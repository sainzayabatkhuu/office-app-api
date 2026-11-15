package com.sol.office.report.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ExportDtos {

    private ExportDtos() {
    }

    public record ExportMonitorResponse(
            Long id,
            String referenceNumber,
            String userId,
            String functionId,
            Integer fetchSize,
            LocalDateTime requestedTime,
            LocalDateTime responseTime,
            String exportStatus,
            String fileName,
            Integer totalRecords) {
    }

    /** Excel export activity in a period: totals and breakdowns by status, function and user. */
    public record ExportSummaryResponse(
            long totalExports,
            long totalRecords,
            List<Bucket> byStatus,
            List<Bucket> byFunction,
            List<Bucket> byUser) {
    }

    public record Bucket(String key, long exports, long records) {
    }

    /** Same shape as office-app-api's NotificationMessage; data holds exportId and referenceNumber. */
    public record ExportResult(String type, String title, String message, java.util.Map<String, String> data) {
    }
}
