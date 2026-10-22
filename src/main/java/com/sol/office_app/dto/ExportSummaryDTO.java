package com.sol.office_app.dto;

import java.util.List;

/** Excel export activity in a period: totals and breakdowns by status, function and user. */
public record ExportSummaryDTO(
        long totalExports,
        long totalRecords,
        List<Bucket> byStatus,
        List<Bucket> byFunction,
        List<Bucket> byUser) {

    public record Bucket(String key, long exports, long records) {
    }
}
