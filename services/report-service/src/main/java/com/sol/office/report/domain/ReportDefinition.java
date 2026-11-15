package com.sol.office.report.domain;

import com.sol.office.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "report_definition")
public class ReportDefinition extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ReportCategory category;

    /** Original name of the uploaded main template (for display/download). */
    @Column(name = "template_file_name", nullable = false)
    private String templateFileName;

    /** Storage key of the main .jrxml of the current version, e.g. {@code templates/HR_LIST/v3/main.jrxml}. */
    @Column(name = "template_storage_key", nullable = false, length = 500)
    private String templateStorageKey;

    @Column(name = "template_version", nullable = false)
    private int templateVersion = 1;

    /** Comma separated {@link ReportFormat} names. */
    @Column(name = "allowed_formats", nullable = false, length = 100)
    private String allowedFormats = "PDF,XLSX";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "default_format", nullable = false, length = 10)
    private ReportFormat defaultFormat = ReportFormat.PDF;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "execution_mode", nullable = false, length = 10)
    private ExecutionMode executionMode = ExecutionMode.SYNC;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportParameter> parameters = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportPermission> permissions = new ArrayList<>();

    public Set<ReportFormat> allowedFormatSet() {
        if (allowedFormats == null || allowedFormats.isBlank()) {
            return EnumSet.noneOf(ReportFormat.class);
        }
        return Arrays.stream(allowedFormats.split(","))
                .map(String::trim)
                .map(ReportFormat::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ReportFormat.class)));
    }

    public void setAllowedFormatSet(Set<ReportFormat> formats) {
        this.allowedFormats = formats.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
