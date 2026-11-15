package com.sol.office.report.engine;

/**
 * A parameter found in a .jrxml template.
 */
public record TemplateParameter(String name, String javaType, String description, String defaultExpression) {
}
