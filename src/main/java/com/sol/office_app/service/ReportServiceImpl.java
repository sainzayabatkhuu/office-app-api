package com.sol.office_app.service;

import com.sol.office_app.dto.ReportDTO;
import com.sol.office_app.dto.ReportParameterDTO;
import com.sol.office_app.entity.Report;
import com.sol.office_app.mapper.ReportDTOMapper;
import com.sol.office_app.repository.ReportRepository;
import com.sol.office_app.util.Utils;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReportDTOMapper reportDTOMapper;

    @Autowired
    private ReportRepository reportRepository;

    @Override
    public Page<ReportDTO> findAll(Pageable pageable) {
        Page<Report> branches = reportRepository.findAll(pageable);
        return branches.map(reportDTOMapper);
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
    public Optional<ReportDTO> save(ReportDTO entity) {
        return Optional.empty();
    }

    @Override
    public Optional<ReportDTO> update(Long aLong, ReportDTO entity) {
        return Optional.empty();
    }

    @Override
    public Optional<ReportDTO> delete(Long aLong, ReportDTO entity) {
        return Optional.empty();
    }

    public Optional<ReportDTO> getDetails(Long id) {
        Optional<Report> reportOpt = reportRepository.findById(id);

        List<ReportParameterDTO> parameterDTOs = new ArrayList<>();

        Report report = reportOpt.get();
        try {
            InputStream reportStream = new ClassPathResource("reports/jrxml/"+report.getFileName()).getInputStream();
            JasperDesign design = JRXmlLoader.load(reportStream);
            JRParameter[] parameters = design.getParameters();

            for (JRParameter param : parameters) {
                if (!param.isSystemDefined()) {

                    String javaType = param.getValueClassName();
                    String inputType = mapJavaTypeToInputType(javaType);
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

    public byte[] exportToFormat(Long id, String format, Map<String, Object> params) throws SQLException, JRException, IOException {
        Optional<Report> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            throw new IllegalArgumentException("Report not found with id: " + id);
        }

        Report report = reportOpt.get();
        InputStream reportStream = new ClassPathResource("reports/jrxml/" + report.getFileName()).getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 🔁 Convert parameters based on expected types
        Map<String, Object> convertedParams = new HashMap<>();

        JasperDesign design = JRXmlLoader.load(new ClassPathResource("reports/jrxml/" + report.getFileName()).getInputStream());
        for (JRParameter jrParam : design.getParameters()) {
            if (!jrParam.isSystemDefined()) {
                String paramName = jrParam.getName();
                Class<?> paramType = jrParam.getValueClass();

                if (params.containsKey(paramName)) {
                    String value = params.get(paramName).toString();
                    Object converted = convertParamValue(value, paramType);
                    convertedParams.put(paramName, converted);
                }
            }
        }

        try (Connection conn = dataSource.getConnection()) {
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, convertedParams, conn);
            return Utils.exportToFormat(jasperPrint, format);
        }
    }

    private String mapJavaTypeToInputType(String javaType) {
        return switch (javaType) {
            case "java.lang.String" -> "text";
            case "java.lang.Integer", "java.lang.Long", "java.math.BigDecimal", "java.lang.Double" -> "number";
            case "java.util.Date", "java.sql.Date" -> "date";
            case "java.sql.Timestamp" -> "datetime-local";
            case "java.lang.Boolean" -> "checkbox";
            default -> "text";
        };
    }

    private Object convertParamValue(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == java.util.Date.class) return java.sql.Date.valueOf(value); // expects yyyy-MM-dd
        if (type == java.sql.Date.class) return java.sql.Date.valueOf(value); // expects yyyy-MM-dd
        if (type == java.sql.Timestamp.class) return Timestamp.valueOf(value); // expects yyyy-MM-dd HH:mm:ss

        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }
}
