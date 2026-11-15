package com.sol.office.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A parameter declared in the template. Extracted on upload; label/required/default/order are then editable.
 */
@Getter
@Setter
@Entity
@Table(name = "report_parameter")
public class ReportParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private ReportDefinition report;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "java_type", nullable = false, length = 100)
    private String javaType;

    /** UI hint: text, number, date, datetime-local, checkbox, multi. */
    @Column(name = "input_type", nullable = false, length = 30)
    private String inputType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    /** Filled by the server from the caller's identity; never accepted from the client. */
    @Column(name = "system_param", nullable = false)
    private boolean systemParam;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
