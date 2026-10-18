package com.sol.office_app.util;

import com.sol.office_app.exception.ApiException;

import com.sol.office_app.config.CustomUserPrincipal;
import com.sol.office_app.entity.Report;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.PathResource;
import org.springframework.http.MediaType;

public class Utils {

    public static MediaType getMediaTypeByFormat(String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "pptx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "odt" -> MediaType.parseMediaType("application/vnd.oasis.opendocument.text");
            case "ods" -> MediaType.parseMediaType("application/vnd.oasis.opendocument.spreadsheet");
            case "csv" -> MediaType.TEXT_PLAIN;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    public static byte[] exportToFormat(JasperPrint jasperPrint, String format) throws JRException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        switch (format.toLowerCase()) {
            case "pdf" -> {
                var exporter = new JRPdfExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();
            }
            case "xlsx" -> {
                var exporter = new JRXlsxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();
            }
            case "pptx" -> {
                var exporter = new JRPptxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();
            }
            case "docx" -> {
                var exporter = new JRDocxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();
            }
            case "odt" -> {
                var exporter = new JROdtExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();
            }


            // Add more formats as needed...
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }

        return outputStream.toByteArray();
    }

    public static String mapJavaTypeToInputType(String javaType) {
        return switch (javaType) {
            case "java.lang.String" -> "text";
            case "java.lang.Integer", "java.lang.Long", "java.math.BigDecimal", "java.lang.Double" -> "number";
            case "java.util.Date", "java.sql.Date" -> "date";
            case "java.sql.Timestamp" -> "datetime-local";
            case "java.lang.Boolean" -> "checkbox";
            default -> "text";
        };
    }

    public static Object convertParamValue(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == java.util.Date.class) return java.sql.Date.valueOf(value); // expects yyyy-MM-dd
        if (type == java.sql.Date.class) return java.sql.Date.valueOf(value); // expects yyyy-MM-dd
        if (type == java.sql.Timestamp.class) return toTimestamp(value);
        if (type == java.math.BigDecimal.class) return new java.math.BigDecimal(value);

        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }

    /** Accepts {@code yyyy-MM-dd HH:mm[:ss]} and the ISO form sent by {@code <input type="datetime-local">}. */
    private static Timestamp toTimestamp(String value) {
        String v = value.replace('T', ' ');
        return Timestamp.valueOf(v.length() == 16 ? v + ":00" : v);
    }

    public static void validateUserAccess(CustomUserPrincipal maker, Report report) {
        boolean hasAccess = report.getRolePermissions() != null && report.getRolePermissions().stream()
                .anyMatch(rp -> maker.getRolePermissions().contains(rp.getRole().getName()));

        if (!hasAccess) {
            throw ApiException.forbidden("You do not have permission to export this report.");
        }
    }

    public static String generateFileName(String reportFileName, String format) {
        return reportFileName.replace(".jrxml", "") + "_" +
                System.currentTimeMillis() + "." + format.toLowerCase();
    }

    public static void compileSubreports(Path mainJrxmlPath) throws IOException, JRException {
        JasperDesign design = JRXmlLoader.load(new PathResource(mainJrxmlPath.toString()).getInputStream());

        Arrays.stream(design.getAllBands())
                .filter(Objects::nonNull)
                .forEach(band -> {
                    if (band.getChildren() != null) {
                        band.getChildren().forEach(el -> {
                            if (el instanceof JRDesignSubreport sub && sub.getExpression() != null) {
                                String subFile = extractSubreportFileName(sub.getExpression().getText());
                                if (subFile.endsWith(".jasper")) {
                                    String subJrxml = subFile.replace(".jasper", ".jrxml");
                                    Path subJrxmlPath = Paths.get(subJrxml).toAbsolutePath();
                                    Path subJasperPath = Paths.get(subFile).toAbsolutePath();

                                    if (Files.exists(subJrxmlPath)) {
                                        try {
                                            JasperCompileManager.compileReportToFile(
                                                    subJrxmlPath.toString(), subJasperPath.toString());
                                        } catch (JRException e) {
                                            throw new RuntimeException("Failed to compile subreport: " + subFile, e);
                                        }
                                    }
                                }
                            }
                        });
                    }
                });
    }

    public static String extractSubreportFileName(String expressionText) {
        String subreportFile = expressionText.replace("\"", "");
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("([^\"]+\\.jasper)").matcher(subreportFile);

        return matcher.find() ? matcher.group(1) : subreportFile;
    }

    public static Map<String, Object> prepareParameters(JasperReport jasperReport, Report report, CustomUserPrincipal maker, Map<String, Object> params) {
        Map<String, Object> convertedParams = new HashMap<>();

        for (JRParameter jrParam : jasperReport.getParameters()) {
            Object raw = params.get(jrParam.getName());
            if (!jrParam.isSystemDefined() && raw != null && !raw.toString().isBlank()) {
                try {
                    convertedParams.put(jrParam.getName(), Utils.convertParamValue(raw.toString().trim(), jrParam.getValueClass()));
                } catch (RuntimeException e) {
                    throw ApiException.badRequest("INVALID_PARAMETER",
                            "Invalid value for parameter '" + jrParam.getName() + "': " + raw);
                }
            }
        }

        convertedParams.put("maker", maker.getName());
        convertedParams.put("reportTitle", report.getTitle());

        return convertedParams;
    }
}
