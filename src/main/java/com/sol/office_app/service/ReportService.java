package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.config.AsyncConfig;
import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import java.util.concurrent.Executor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office_app.domain.response.NotificationMessage;
import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.dto.ReportParameterDTO;
import com.sol.office_app.entity.*;
import com.sol.office_app.enums.ReportStatus;
import com.sol.office_app.mapper.ReportDTOMapper;
import com.sol.office_app.repository.ReportHistoryRepository;
import com.sol.office_app.repository.ReportRepository;
import com.sol.office_app.repository.RoleRepository;
import com.sol.office_app.repository.UserRepository;
import com.sol.office_app.util.Utils;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService implements GeneralService<ReportDTO, Long> {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private ReportDTOMapper reportDTOMapper;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportHistoryRepository reportHistoryRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private NotifierService notifierService;
    @Autowired
    @Qualifier(AsyncConfig.REPORT_EXECUTOR)
    private Executor reportExecutor;
    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final Set<String> SUPPORTED_FORMATS = Set.of("pdf", "xlsx", "docx", "pptx", "odt");
    /** {@link ReportRolePermission#getRunInBackground()} values. */
    private static final String RUN_NOW = "V";
    private static final String RUN_IN_BACKGROUND = "B";

    @Value("${report.subreport.dir}")
    private String subreportDir;

    @Value("${report.output.dir}")
    private String outputDir;

    @Override
    public Page<ReportDTO> findAll(Pageable pageable) {
        Page<Report> reports = reportRepository.findAll(pageable);
        return reports.map(reportDTOMapper);
    }

    @Override
    public Page<ReportDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<ReportDTO> get(Long id) {
        Optional<Report> report = reportRepository.findById(id);
        return report.map(reportDTOMapper);
    }

    @Override
    public Optional<ReportDTO> save(ReportDTO dto) {

        return Optional.empty();
    }

    @Override
    public Optional<ReportDTO> update(Long aLong, ReportDTO entity) {

        return Optional.empty();
    }

    public Optional<ReportDTO> save(String title, MultipartFile file) {

        Report entity = new Report();
        if(!file.isEmpty()) {
            try {
                Path path = Paths.get(subreportDir).toAbsolutePath();
                PathResource resource = new PathResource(path);
                File reportsDir = resource.getFile();

                // Ensure the directory exists
                if (!reportsDir.exists()) {
                    reportsDir.mkdirs();
                }
                File fileToSave = new File(reportsDir, file.getOriginalFilename());

                // Save the uploaded file
                file.transferTo(fileToSave);
                entity.setFileName(file.getOriginalFilename());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to store file.");
            }
        }
        entity.setTitle(title);

        ReportRolePermission reportRolePermission = new ReportRolePermission();
        reportRolePermission.setBranch(null);
        reportRolePermission.setRunInBackground("V");
        Optional<Role> adminRoleOpt = roleRepository.findByName("ROLE_ADMIN");
        adminRoleOpt.ifPresent(reportRolePermission::setRole);
        reportRolePermission.setReport(entity);

        if (entity.getRolePermissions() == null) {
            entity.setRolePermissions(new ArrayList<>());
        }

        entity.getRolePermissions().add(reportRolePermission);
        Report saved = reportRepository.save(entity);
        return Optional.of(reportDTOMapper.apply(saved));
    }

    public Optional<ReportDTO> update(String title,String name, MultipartFile file) {

        Report entity = reportRepository.finnByFileName(name);
        if (entity == null) {
            return Optional.empty();
        }
        if (file != null && !file.isEmpty()) {
            try {
                Path path = Paths.get(subreportDir).toAbsolutePath();
                PathResource resource = new PathResource(path);
                File reportsDir = resource.getFile();

                // Ensure the directory exists
                if (!reportsDir.exists()) {
                    reportsDir.mkdirs();
                }
                File fileToSave = new File(reportsDir, file.getOriginalFilename());

                // Save the uploaded file
                file.transferTo(fileToSave);
                entity.setFileName(file.getOriginalFilename());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to store file.");
            }
        }
        entity.setTitle(title);
        Report saved = reportRepository.save(entity);
        return Optional.of(reportDTOMapper.apply(saved));
    }

    @Override
    public void delete(Long aLong) {
        reportRepository.deleteById(aLong);
    }

    public Page<ReportDTO> getList(Authentication authentication, Pageable pageable) {

        CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();
        if (user.getRolePermissions().isEmpty()) {
            return Page.empty(pageable);
        }

        List<Role> roles = roleRepository.findByNameIn(user.getRolePermissions());
        if (roles.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        Page<Report> reports = reportRepository.findReportsByUserRoleIds(roleIds,pageable);
        return reports.map(reportDTOMapper);
    }

    public Optional<ReportDTO> getDetails(Long id) {
        Optional<Report> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            return Optional.empty();
        }

        List<ReportParameterDTO> parameterDTOs = new ArrayList<>();

        Report report = reportOpt.get();
        try {
            Path path = Paths.get(subreportDir, report.getFileName()).toAbsolutePath();
            PathResource resource = new PathResource(path);
            InputStream reportStream = resource.getInputStream();

            JasperDesign design = JRXmlLoader.load(reportStream);
            JRParameter[] parameters = design.getParameters();

            for (JRParameter param : parameters) {
                if (!param.isSystemDefined()) {

                    String javaType = param.getValueClassName();
                    String inputType = Utils.mapJavaTypeToInputType(javaType);
                    String description = (param.getDescription() != null && !param.getDescription().isBlank())
                            ? param.getDescription()
                            : param.getName();
                    String defualtValue = "";

                    if(param.getName().equals("reportTitle")) {
                        defualtValue = report.getTitle();
                        inputType = "hidden";
                    }

                    if(param.getName().equals("branchId")) {
                        defualtValue = "000";
                    }

                    if(param.getName().equals("maker")) {
                        defualtValue = "reporter"; // logged user info
                        inputType = "hidden";
                    }
                    parameterDTOs.add(new ReportParameterDTO(param.getName(), javaType, inputType, description, defualtValue));
                }
            }

        } catch (JRException | IOException e) {
            throw new RuntimeException("Failed to load parameters from jrxml", e);
        }

        return reportOpt.map(r -> {
            ReportDTO dto = reportDTOMapper.apply(r);
            return new ReportDTO(
                    dto.id(),
                    dto.name(),
                    dto.title(),
                    dto.rolePermissions(),
                    parameterDTOs
            );
        });
    }

    /**
     * Generates a report. "V" permissions run it within the request; "B" permissions queue it on the
     * report executor and notify the user over WebSocket when it finishes. The output is downloaded
     * through {@code GET /report-histories/{id}/download}.
     */
    public NotificationMessage exportToFormat(
            Authentication authentication,
            Long id,
            String format,
            Map<String, Object> params
    ) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Report", id));
        CustomUserPrincipal maker = (CustomUserPrincipal) authentication.getPrincipal();
        Utils.validateUserAccess(maker, report);
        if (format == null || !SUPPORTED_FORMATS.contains(format.toLowerCase())) {
            throw ApiException.badRequest("FORMAT_NOT_SUPPORTED", "Unsupported format: " + format);
        }

        String reportFileName = report.getFileName();
        String generatedFileName = Utils.generateFileName(reportFileName, format);
        Path jrxmlPath = Paths.get(subreportDir, reportFileName).toAbsolutePath();
        Path jasperPath = jrxmlPath.resolveSibling(reportFileName.replace(".jrxml", ".jasper"));
        Path outputPath = Paths.get(outputDir, maker.getName()).toAbsolutePath().resolve(generatedFileName);

        ReportHistory history = new ReportHistory();
        history.setBranchCode(maker.getBranch());
        history.setReportFileName(reportFileName);
        history.setReportTitleName(report.getTitle());
        history.setStatus(ReportStatus.PROCESSING);
        history.setStartedAt(LocalDateTime.now());
        history.setRunInBackground(report.getRolePermissions().stream()
                .filter(rp -> maker.getRolePermissions().contains(rp.getRole().getName()))
                .map(ReportRolePermission::getRunInBackground)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(RUN_NOW));
        history.setReportOutputFile(generatedFileName);
        history.setParameters(toJson(params));
        reportHistoryRepository.save(history);

        JasperReport jasperReport;
        Map<String, Object> fillParams;
        try {
            Utils.compileSubreports(jrxmlPath);
            JasperCompileManager.compileReportToFile(jrxmlPath.toString(), jasperPath.toString());
            jasperReport = (JasperReport) JRLoader.loadObjectFromFile(jasperPath.toString());
            fillParams = Utils.prepareParameters(jasperReport, report, maker, params);
            fillParams.put("SUBREPORT_DIR", subreportDir);
        } catch (ApiException e) {
            finish(history, ReportStatus.FAILED);
            throw e;
        } catch (Exception e) {
            log.error("Report {} could not be prepared", reportFileName, e);
            finish(history, ReportStatus.FAILED);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REPORT_TEMPLATE_INVALID",
                    "The report template could not be compiled.");
        }

        // mode: V = generated now (download right away), B = queued (watch the history / notifications)
        Map<String, String> data = Map.of("historyId", String.valueOf(history.getId()), "fileName", generatedFileName,
                "mode", history.getRunInBackground());

        if (!RUN_IN_BACKGROUND.equals(history.getRunInBackground())) {
            try {
                fillAndWrite(jasperReport, fillParams, format, outputPath);
            } catch (Exception e) {
                log.error("Report {} failed", reportFileName, e);
                finish(history, ReportStatus.FAILED);
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REPORT_GENERATION_FAILED",
                        "The report could not be generated.");
            }
            finish(history, ReportStatus.SUCCESS);
            return new NotificationMessage("success", "Export Successful",
                    "The report has been generated successfully.", data);
        }

        String username = maker.getName();
        reportExecutor.execute(() -> {
            try {
                fillAndWrite(jasperReport, fillParams, format, outputPath);
                finish(history, ReportStatus.SUCCESS);
                notifierService.notifyUser(username, new NotificationMessage("success", report.getTitle(),
                        "Background report has been generated.", data));
            } catch (Exception e) {
                log.error("Background report {} failed", reportFileName, e);
                finish(history, ReportStatus.FAILED);
                notifierService.notifyUser(username, new NotificationMessage("error", report.getTitle(),
                        "Background report failed.", data));
            }
        });

        return new NotificationMessage("success", "Export Scheduled",
                "The report export has been scheduled in the background.", data);
    }

    /** The user's parameter values as submitted (request plumbing like id/format left out), for the history. */
    private String toJson(Map<String, Object> params) {
        Map<String, Object> userParams = new TreeMap<>(params);
        userParams.keySet().removeAll(Set.of("id", "format"));
        try {
            return objectMapper.writeValueAsString(userParams);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** Fills against the database and writes the output; the connection is always returned to the pool. */
    private void fillAndWrite(JasperReport jasperReport, Map<String, Object> params, String format, Path outputPath)
            throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(params), conn);
            byte[] bytes = Utils.exportToFormat(jasperPrint, format);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, bytes);
        }
    }

    private void finish(ReportHistory history, ReportStatus status) {
        history.setStatus(status);
        history.setFinishedAt(LocalDateTime.now());
        reportHistoryRepository.save(history);
    }

    /** Reports the caller's roles may run, optionally filtered by title / file name. */
    public Page<ReportDTO> findDynamic(String title, String fileName, Authentication authentication, Pageable pageable) {
        CustomUserPrincipal maker = (CustomUserPrincipal) authentication.getPrincipal();
        if (maker.getRolePermissions().isEmpty()) {
            return Page.empty(pageable);
        }
        return reportRepository.findReportsByUserRoleNames(maker.getRolePermissions(),
                title == null ? "" : title.trim(), fileName == null ? "" : fileName.trim(), pageable)
                .map(reportDTOMapper);
    }
}
