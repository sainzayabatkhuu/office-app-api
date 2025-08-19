package com.sol.office_app.dto;

import org.springframework.web.multipart.MultipartFile;

public record ReportUploadDTO(
        String title,
        MultipartFile file
) {
}
