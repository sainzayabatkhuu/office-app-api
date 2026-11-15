package com.sol.office.report.domain;

import org.springframework.http.MediaType;

public enum ReportFormat {
    PDF("pdf", MediaType.APPLICATION_PDF_VALUE),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    CSV("csv", "text/csv"),
    HTML("html", MediaType.TEXT_HTML_VALUE);

    private final String extension;
    private final String contentType;

    ReportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }
}
