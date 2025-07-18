package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.service.ReportServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static com.sol.office_app.util.Utils.getMediaTypeByFormat;

@RestController
@RequestMapping(Constant.REPORT_URL_PREFIX)
@CrossOrigin("*")
public class ReportController {

    private final ReportServiceImpl reportService;

    public ReportController(ReportServiceImpl reportService) {
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

    @PostMapping
    public ResponseEntity<ReportDTO> save(@RequestBody ReportDTO reportDTO) {
        Optional<ReportDTO> savedBranch = reportService.save(reportDTO);
        return savedBranch.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ReportDTO> update(@PathVariable Long id, @RequestBody ReportDTO reportDTO) {
        Optional<ReportDTO> updatedBranchOpt = reportService.update(id, reportDTO);
        return updatedBranchOpt
                .map(branchDTO -> new ResponseEntity<>(branchDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ReportDTO> delete(@PathVariable Long id, @RequestBody ReportDTO reportDTO) {
        Optional<ReportDTO> updatedBranchOpt = reportService.delete(id, reportDTO);
        return updatedBranchOpt
                .map(branchDTO -> new ResponseEntity<>(branchDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
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
    public ResponseEntity<byte[]> generatePdfReport(
            @RequestParam String format,
            @RequestParam Long id,
            @RequestParam Map<String, Object> params
    ) throws Exception {
        byte[] pdfData = reportService.exportToFormat(id, format, params);

        MediaType mediaType = getMediaTypeByFormat(format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=report.pdf") // 👈 inline instead of attachment
                .contentType(mediaType)
                .body(pdfData);
    }
}
