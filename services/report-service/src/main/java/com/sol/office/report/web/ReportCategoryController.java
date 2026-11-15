package com.sol.office.report.web;

import com.sol.office.report.dto.ReportDtos.CategoryResponse;
import com.sol.office.report.service.ReportCatalogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reports")
@RestController
@RequiredArgsConstructor
public class ReportCategoryController {

    private final ReportCatalogService catalogService;

    @GetMapping("/api/v1/report-categories")
    public List<CategoryResponse> list() {
        return catalogService.listCategories();
    }
}
