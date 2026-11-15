package com.sol.office.report.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.report.config.DataSourceConfig;
import com.sol.office.report.config.ReportProperties;
import com.sol.office.report.domain.ReportFormat;
import com.sol.office.report.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.query.JRJdbcQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.governors.MaxPagesGovernor;
import net.sf.jasperreports.governors.TimeoutGovernor;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.repo.FileRepositoryService;
import net.sf.jasperreports.repo.RepositoryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * JasperReports integration. Templates are compiled once at upload time (so a broken template is rejected
 * immediately, not when a user runs it) and the compiled form is cached in memory.
 */
@Slf4j
@Component
public class JasperReportEngine {

    private static final String JRXML = ".jrxml";
    private static final String JASPER = ".jasper";

    private final FileStorage storage;
    private final DataSource reportingDataSource;
    private final JasperReportsContext baseContext;
    private final Cache<String, JasperReport> compiledCache = Caffeine.newBuilder().maximumSize(500).build();

    public JasperReportEngine(FileStorage storage,
                              @Qualifier(DataSourceConfig.REPORTING_DATA_SOURCE) DataSource reportingDataSource,
                              ReportProperties properties) {
        this.storage = storage;
        this.reportingDataSource = reportingDataSource;
        this.baseContext = createBaseContext(properties.execution());
    }

    /**
     * Compiles every .jrxml in the template's directory (main report + subreports) and returns the
     * main report's user-facing parameters.
     */
    public List<TemplateParameter> compileTemplate(String mainTemplateKey) {
        Path main = storage.localPath(mainTemplateKey);
        try (Stream<Path> files = Files.walk(main.getParent())) {
            for (Path jrxml : files.filter(f -> f.toString().endsWith(JRXML)).toList()) {
                try {
                    JasperCompileManager.getInstance(baseContext)
                            .compileToFile(jrxml.toString(), jasperPath(jrxml).toString());
                } catch (JRException e) {
                    throw new BusinessException("INVALID_TEMPLATE",
                            "Template " + jrxml.getFileName() + " does not compile: " + rootMessage(e));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        JasperReport report = load(mainTemplateKey);
        return Arrays.stream(report.getParameters())
                .filter(p -> !p.isSystemDefined() && p.isForPrompting())
                .map(p -> new TemplateParameter(p.getName(), p.getValueClassName(), p.getDescription(),
                        p.getDefaultValueExpression() == null ? null : p.getDefaultValueExpression().getText()))
                .toList();
    }

    /**
     * Fills the report against the reporting database and exports it. Blocking and potentially slow:
     * callers are responsible for limiting concurrency.
     */
    public RenderedReport render(String mainTemplateKey, Map<String, Object> parameters, ReportFormat format) {
        JasperReport report = compiledCache.get(mainTemplateKey, this::load);
        Path templateDir = storage.localPath(mainTemplateKey).getParent();
        JasperReportsContext context = contextFor(templateDir);

        Map<String, Object> fillParams = new HashMap<>(parameters);
        // Compatibility with templates written for office-app-api: $P{SUBREPORT_DIR} + "sub.jasper"
        fillParams.put("SUBREPORT_DIR", templateDir + File.separator);

        try (Connection connection = reportingDataSource.getConnection()) {
            JasperPrint print = JasperFillManager.getInstance(context).fill(report, fillParams, connection);
            return new RenderedReport(export(context, print, format), print.getPages().size());
        } catch (JRException e) {
            throw new ReportGenerationException("Report generation failed: " + rootMessage(e), e);
        } catch (SQLException e) {
            throw new ReportGenerationException("Reporting database unavailable: " + e.getMessage(), e);
        }
    }

    private JasperReport load(String mainTemplateKey) {
        Path jasper = jasperPath(storage.localPath(mainTemplateKey));
        try {
            return (JasperReport) JRLoader.loadObject(jasper.toFile());
        } catch (JRException e) {
            throw new ReportGenerationException("Compiled template missing: " + jasper.getFileName(), e);
        }
    }

    private byte[] export(JasperReportsContext context, JasperPrint print, ReportFormat format) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SimpleExporterInput input = new SimpleExporterInput(print);
        switch (format) {
            case PDF -> {
                JRPdfExporter exporter = new JRPdfExporter(context);
                exporter.setExporterInput(input);
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
                exporter.exportReport();
            }
            case XLSX -> {
                JRXlsxExporter exporter = new JRXlsxExporter(context);
                SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
                config.setDetectCellType(true);
                config.setRemoveEmptySpaceBetweenRows(true);
                config.setRemoveEmptySpaceBetweenColumns(true);
                config.setWhitePageBackground(false);
                exporter.setConfiguration(config);
                exporter.setExporterInput(input);
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
                exporter.exportReport();
            }
            case DOCX -> {
                JRDocxExporter exporter = new JRDocxExporter(context);
                exporter.setExporterInput(input);
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
                exporter.exportReport();
            }
            case CSV -> {
                out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // UTF-8 BOM so Excel reads Cyrillic
                JRCsvExporter exporter = new JRCsvExporter(context);
                exporter.setExporterInput(input);
                exporter.setExporterOutput(new SimpleWriterExporterOutput(out, StandardCharsets.UTF_8.name()));
                exporter.exportReport();
            }
            case HTML -> {
                HtmlExporter exporter = new HtmlExporter(context);
                exporter.setExporterInput(input);
                exporter.setExporterOutput(new SimpleHtmlExporterOutput(out, StandardCharsets.UTF_8.name()));
                exporter.exportReport();
            }
        }
        return out.toByteArray();
    }

    /** Resolves images/subreports referenced by relative path against the template's own directory. */
    private JasperReportsContext contextFor(Path templateDir) {
        SimpleJasperReportsContext context = new SimpleJasperReportsContext(baseContext);
        List<RepositoryService> repositories = new ArrayList<>();
        repositories.add(new FileRepositoryService(context, templateDir.toString(), true));
        repositories.addAll(baseContext.getExtensions(RepositoryService.class));
        context.setExtensions(RepositoryService.class, repositories);
        return context;
    }

    /** Safety limits applied to every report regardless of what the template says. */
    private static JasperReportsContext createBaseContext(ReportProperties.Execution limits) {
        SimpleJasperReportsContext context = new SimpleJasperReportsContext(DefaultJasperReportsContext.getInstance());
        context.setProperty(MaxPagesGovernor.PROPERTY_MAX_PAGES_ENABLED, "true");
        context.setProperty(MaxPagesGovernor.PROPERTY_MAX_PAGES, String.valueOf(limits.maxPages()));
        context.setProperty(TimeoutGovernor.PROPERTY_TIMEOUT_ENABLED, "true");
        context.setProperty(TimeoutGovernor.PROPERTY_TIMEOUT, String.valueOf(limits.fillTimeout().toMillis()));
        context.setProperty(JRJdbcQueryExecuterFactory.PROPERTY_JDBC_QUERY_TIMEOUT,
                String.valueOf(limits.fillTimeout().toSeconds()));
        context.setProperty(JRJdbcQueryExecuterFactory.PROPERTY_JDBC_FETCH_SIZE, "1000");
        return context;
    }

    private static Path jasperPath(Path jrxml) {
        String name = jrxml.getFileName().toString();
        return jrxml.resolveSibling(name.substring(0, name.length() - JRXML.length()) + JASPER);
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
