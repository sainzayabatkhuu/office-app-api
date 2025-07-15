package com.sol.office_app.util;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.io.ByteArrayOutputStream;
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
}
