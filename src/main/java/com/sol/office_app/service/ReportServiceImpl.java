package com.sol.office_app.service;

import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.dto.NotificationMessage;
import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.dto.ReportParameterDTO;
import com.sol.office_app.entity.Report;
import com.sol.office_app.entity.Role;
import com.sol.office_app.mapper.ReportDTOMapper;
import com.sol.office_app.repository.ReportRepository;
import com.sol.office_app.repository.RoleRepository;
import com.sol.office_app.repository.UserRepository;
import com.sol.office_app.util.Utils;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private ReportDTOMapper reportDTOMapper;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private NotifierService notifierService;

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

    public Optional<ReportDTO> save(String title, MultipartFile file) {

        try {
            Path path = Paths.get("uploads/reports/jrxml").toAbsolutePath();
            PathResource resource = new PathResource(path);
            File reportsDir = resource.getFile();

            // Ensure the directory exists
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            File fileToSave = new File(reportsDir, file.getOriginalFilename());

            // Save the uploaded file
            file.transferTo(fileToSave);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            //throw new IllegalAccessException("Failed to store file.", e);
        }

        Report entity = new Report();
        entity.setTitle(title);
        entity.setFileName(file.getOriginalFilename());
        Report saved = reportRepository.save(entity);
        return Optional.of(reportDTOMapper.apply(saved));
    }

    @Override
    public Optional<ReportDTO> update(Long aLong, ReportDTO entity) {
        return Optional.empty();
    }

    @Override
    public Optional<ReportDTO> delete(Long aLong, ReportDTO entity) {
        return Optional.empty();
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
            Path path = Paths.get("uploads/reports/jrxml", report.getFileName()).toAbsolutePath();
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
    ) throws SQLException, JRException, IOException {
        Optional<Report> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            throw new IllegalArgumentException("Report not found with id: " + id);
        }

        CustomUserPrincipal maker = (CustomUserPrincipal) authentication.getPrincipal();
        if (maker.getRules().isEmpty()) {
            throw new IllegalArgumentException("User has no access rules.");
        }

        Report report = reportOpt.get();
        if (report.getRolePermissions().isEmpty()) {
            throw new IllegalArgumentException("Report has no role permissions.");
        }
        System.out.println(maker.getRolePermissions());
        System.out.println(report.getRolePermissions());
        boolean hasAccess = report.getRolePermissions().stream()
                .anyMatch(rp -> maker.getRolePermissions().contains(rp.getRole().getName()));
        if (!hasAccess) {
            throw new IllegalArgumentException("You do not have permission to export this report.");
        }

        String reportFileName = report.getFileName();
        String generatedFileName = reportFileName.replace(".jrxml", "") + "_" + System.currentTimeMillis() + "." + format.toLowerCase();

        Path path = Paths.get("uploads/reports/jrxml", reportFileName).toAbsolutePath();
        try (InputStream reportStream = new PathResource(path).getInputStream()) {
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> convertedParams = new HashMap<>();
            JasperDesign design = JRXmlLoader.load(new PathResource("uploads/reports/jrxml/" + reportFileName).getInputStream());

            for (JRParameter jrParam : design.getParameters()) {
                if (!jrParam.isSystemDefined()) {
                    String paramName = jrParam.getName();
                    Class<?> paramType = jrParam.getValueClass();

                    if (params.containsKey(paramName)) {
                        String value = params.get(paramName).toString();
                        Object converted = Utils.convertParamValue(value, paramType);
                        convertedParams.put(paramName, converted);
                    }
                }
            }

            convertedParams.put("maker", maker.getName());
            convertedParams.put("reportTitle", report.getTitle());

            try (Connection conn = dataSource.getConnection()) {
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, convertedParams, conn);

                Path userDir = Paths.get("generated_reports", maker.getName());
                Files.createDirectories(userDir);
                Path outputPath = userDir.resolve(generatedFileName);

                byte[] data = Utils.exportToFormat(jasperPrint, format);
                Files.write(outputPath, data);

                //notifierService.runNotifier("regular", "", "Background report process was invoked and successfully completed.");

                return new NotificationMessage(
                        "success",
                        "Export Successful",
                        "The report has been generated successfully.",
                        Map.of("fileName", generatedFileName)
                );
            }
        }
    }
}
