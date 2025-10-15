package com.sol.office_app.dto;

import jakarta.persistence.Column;

import java.time.LocalDateTime;

public record ExportMonitorDTO(
        Long id,
        String referenceNumber,
        String userId,
        String functionId,
        Integer fetchSize,
        LocalDateTime requestedTime,
        LocalDateTime responseTime,
        String exportStatus,
        String fileName,
        Integer totalRecords
) {
}
