package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.NotificationMessage;
import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping(Constant.REPORT_URL_PREFIX)
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ReportDTO> index(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size){
        return reportService.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ReportDTO> get(@PathVariable("id") Long id){
        return reportService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportDTO> save(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file
    ) {
        Optional<ReportDTO> savedBranch = reportService.save(title, file);
        return savedBranch.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportDTO> update(
            @RequestParam("title") String title,
            @RequestParam("name") String name,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Optional<ReportDTO> updatedBranchOpt = reportService.update(title, name, file);
        return updatedBranchOpt
                .map(branchDTO -> new ResponseEntity<>(branchDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        reportService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/report-list", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ReportDTO> getList(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Authentication authentication){
        return reportService.getList(authentication, PageRequest.of(page, size));
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<ReportDTO> details(@PathVariable("id") Long id){
        return reportService.getDetails(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/export")
    public ResponseEntity<NotificationMessage> generatePdfReport(
            @RequestParam String format,
            @RequestParam Long id,
            @RequestParam Map<String, Object> params,
            Authentication authentication
    ) throws Exception {
        NotificationMessage result = reportService.exportToFormat(authentication, id, format, params);
        return ResponseEntity.ok()
                .body(result);
    }
}
