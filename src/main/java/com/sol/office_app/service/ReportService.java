package com.sol.office_app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office_app.common.GeneralService;
import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.dto.NotificationMessage;
import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.dto.ReportHistoryDto;
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
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.annotation.Async;
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
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
        System.out.println("start -------------------");

        CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();

        List<Role> roles = roleRepository.findByNameIn(user.getRolePermissions());
        System.out.println(roles);

        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        System.out.println("Roles: " + roleIds);
        System.out.println("end -------------------");
        Page<Report> reports = reportRepository.findReportsByUserRoleIds(roleIds,pageable);
        return reports.map(reportDTOMapper);
    }

    public Optional<ReportDTO> getDetails(Long id) {
        Optional<Report> reportOpt = reportRepository.findById(id);

        List<ReportParameterDTO> parameterDTOs = new ArrayList<>();

        Report report = reportOpt.get();
        try {
            Path path = Paths.get("uploads/reports/jrxml/", report.getFileName()).toAbsolutePath();
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

    public NotificationMessage exportToFormat(
            Authentication authentication,
            Long id,
            String format,
            Map<String, Object> params
    ) throws Exception {

        // Step 1: Validate Report & User
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + id));

        CustomUserPrincipal maker = (CustomUserPrincipal) authentication.getPrincipal();
        Utils.validateUserAccess(maker, report);

        // Step 2: Prepare Report File Names
        String reportFileName = report.getFileName();
        String generatedFileName = Utils.generateFileName(reportFileName, format);
        Path jrxmlPath = Paths.get(subreportDir, reportFileName).toAbsolutePath();
        Path jasperPath = jrxmlPath.resolveSibling(reportFileName.replace(".jrxml", ".jasper"));

        // Step 3: Initialize Report History
        //ReportHistory history = createReportHistory(report, maker);

        ReportHistory finalHistory = new ReportHistory();
        finalHistory.setBranchCode("000");
        finalHistory.setReportFileName(report.getFileName());
        finalHistory.setReportTitleName(report.getTitle());
        finalHistory.setStatus(ReportStatus.PROCESSING);
        finalHistory.setStartedAt(LocalDateTime.now());
        report.getRolePermissions().stream()
                .filter(rp -> maker.getRolePermissions().contains(rp.getRole().getName()))
                .findFirst()
                .ifPresent(rp -> finalHistory.setRunInBackground(rp.getRunInBackground()));

        reportHistoryRepository.save(finalHistory);

        try (InputStream reportStream = new PathResource(jrxmlPath).getInputStream()) {

            // Step 4: Compile Report & Subreports
            Utils.compileSubreports(jrxmlPath);
            JasperCompileManager.compileReportToFile(jrxmlPath.toString(), jasperPath.toString());

            JasperReport jasperReport = (JasperReport) JRLoader.loadObjectFromFile(jasperPath.toString());

            // Step 5: Prepare Parameters
            Map<String, Object> convertedParams = Utils.prepareParameters(report, maker, params);

            Connection conn = dataSource.getConnection();
            convertedParams.put("REPORT_CONNECTION", conn);
            convertedParams.put("SUBREPORT_DIR", subreportDir);
            // -----------------------------
            // Step 6: Export file
            // -----------------------------
            String runInBackground = "B"; // "Virtual" | "Background"

            if(runInBackground.equals("V")) {

                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, convertedParams, conn);
                finalHistory.setReportOutputFile(generatedFileName);
                finalHistory.setFinishedAt(LocalDateTime.now());
                reportHistoryRepository.save(finalHistory);

                Path userDir = Paths.get(outputDir, maker.getName());
                Files.createDirectories(userDir);
                Path outputPath = userDir.resolve(generatedFileName);
                byte[] data = Utils.exportToFormat(jasperPrint, format);
                Files.write(outputPath, data);


                return new NotificationMessage(
                        "success",
                        "Export Successful",
                        "The report has been generated successfully.",
                        Map.of("fileName", generatedFileName)
                );
            } else {
                //calling CompletableFuture

                CompletableFuture.runAsync(() -> {
                    try {

                        // Wait for 1 minute before starting the background export
                        Thread.sleep(60 * 1000);

                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, convertedParams, conn);
                        finalHistory.setStatus(ReportStatus.SUCCESS);
                        finalHistory.setReportOutputFile(generatedFileName);
                        finalHistory.setFinishedAt(LocalDateTime.now());
                        reportHistoryRepository.save(finalHistory);

                        Path userDir = Paths.get(outputDir, maker.getName()).toAbsolutePath();
                        Files.createDirectories(userDir);
                        Path outputPath = userDir.resolve(generatedFileName);
                        byte[] data = Utils.exportToFormat(jasperPrint, format);
                        Files.write(outputPath, data);

                        notifierService.runNotifier("regular", "", "Background report process was invoked and successfully completed.");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });


                return new NotificationMessage(
                        "success",
                        "Export Scheduled",
                        "The report export has been scheduled in the background.",
                        Map.of()
                );
            }
        } finally {
            //reportHistoryRepository.save(history);
        }
    }

    public Page<ReportDTO> findDynamic (String title, String fileName,Authentication authentication, Pageable pageable) {
        // Spring Security-аар нэвтэрсэн хэрэглэгчийн эрх авах
        CustomUserPrincipal maker = (CustomUserPrincipal) authentication.getPrincipal();
        System.out.println(maker.getRolePermissions());
        System.out.println(maker.getRules());
//        String roleCode = authentication.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .findFirst()
//                .orElse(() -> new IllegalArgumentException("Report not found with id: " ));


        Page<Report> reports = reportRepository.findReportsByUserRoleNames(maker.getRolePermissions(), pageable);
        return reports.map(reportDTOMapper);
    }
}
