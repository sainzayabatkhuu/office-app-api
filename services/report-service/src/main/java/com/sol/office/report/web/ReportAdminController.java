package com.sol.office.report.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.report.dto.ReportDtos.CategoryRequest;
import com.sol.office.report.dto.ReportDtos.CategoryResponse;
import com.sol.office.report.dto.ReportDtos.ParameterUpdate;
import com.sol.office.report.dto.ReportDtos.PermissionRequest;
import com.sol.office.report.dto.ReportDtos.PermissionResponse;
import com.sol.office.report.dto.ReportDtos.ReportCreateRequest;
import com.sol.office.report.dto.ReportDtos.ReportDetail;
import com.sol.office.report.dto.ReportDtos.ReportMetadataRequest;
import com.sol.office.report.dto.ReportDtos.ReportSummary;
import com.sol.office.report.service.ReportCatalogService;
import com.sol.office.report.service.ReportCatalogService.TemplateFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Report administration (REPORT_MANAGE): templates, metadata, parameters, permissions, categories.
 */
@Tag(name = "Report administration")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REPORT_MANAGE')")
public class ReportAdminController {

    private final ReportCatalogService catalogService;

    @GetMapping("/reports/manage")
    public PageResponse<ReportSummary> search(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Boolean active,
                                              @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return catalogService.search(q, active, pageable);
    }

    @GetMapping("/reports/manage/{id}")
    public ReportDetail get(@PathVariable Long id) {
        return catalogService.getForAdmin(id);
    }

    @Operation(summary = "Create a report from a .jrxml (or .zip with subreports)",
            description = "Multipart: part 'report' (JSON ReportCreateRequest) + part 'file'. "
                    + "For a zip with several .jrxml files pass 'mainTemplate' or name the main one main.jrxml.")
    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDetail create(@Valid @RequestPart("report") ReportCreateRequest request,
                               @RequestPart("file") MultipartFile file,
                               @RequestParam(required = false) String mainTemplate) {
        return catalogService.create(request, file, mainTemplate);
    }

    @PutMapping("/reports/{id}")
    public ReportDetail updateMetadata(@PathVariable Long id, @Valid @RequestBody ReportMetadataRequest request) {
        return catalogService.updateMetadata(id, request);
    }

    @Operation(summary = "Upload a new template version")
    @PostMapping(value = "/reports/{id}/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportDetail uploadTemplate(@PathVariable Long id,
                                      @RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false) String mainTemplate) {
        return catalogService.uploadTemplate(id, file, mainTemplate);
    }

    @GetMapping("/reports/{id}/template")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable Long id) {
        TemplateFile file = catalogService.downloadTemplate(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_XML)
                .body(file.resource());
    }

    @PutMapping("/reports/{id}/parameters")
    public ReportDetail updateParameters(@PathVariable Long id, @Valid @RequestBody List<@Valid ParameterUpdate> updates) {
        return catalogService.updateParameters(id, updates);
    }

    @PostMapping("/reports/{id}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse addPermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        return catalogService.addPermission(id, request);
    }

    @DeleteMapping("/reports/{id}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePermission(@PathVariable Long id, @PathVariable Long permissionId) {
        catalogService.removePermission(id, permissionId);
    }

    @PostMapping("/report-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalogService.createCategory(request);
    }

    @PutMapping("/report-categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalogService.updateCategory(id, request);
    }
}
