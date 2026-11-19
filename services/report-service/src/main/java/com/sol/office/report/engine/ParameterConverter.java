package com.sol.office.report.engine;

import com.sol.office.common.exception.BusinessException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Converts the string values the UI submits into the Java types the template declares.
 * Dates are ISO (yyyy-MM-dd, yyyy-MM-ddTHH:mm[:ss]); collections are comma separated.
 */
public final class ParameterConverter {

    /** Template parameter class for a set of branch codes; the UI shows a branch picker for it. */
    public static final String BRANCH_LIST_TYPE = "com.sol.jasper.params.BranchList";
    /** Input type sent to the UI for {@link #BRANCH_LIST_TYPE} parameters. */
    public static final String BRANCH_LIST_INPUT = "BranchList";

    private ParameterConverter() {
    }

    public static boolean isBranchList(String javaType) {
        return BRANCH_LIST_TYPE.equals(javaType);
    }

    public static String inputTypeFor(String javaType) {
        return switch (javaType) {
            case BRANCH_LIST_TYPE -> BRANCH_LIST_INPUT;
            case "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.math.BigDecimal",
                 "java.lang.Double", "java.lang.Float" -> "number";
            case "java.util.Date", "java.sql.Date", "java.time.LocalDate" -> "date";
            case "java.sql.Timestamp", "java.time.LocalDateTime" -> "datetime-local";
            case "java.lang.Boolean" -> "checkbox";
            case "java.util.List", "java.util.Collection", "java.util.Set" -> "multi";
            default -> "text";
        };
    }

    public static Object convert(String name, String value, String javaType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            return switch (javaType) {
                case "java.lang.String", "java.lang.Object" -> v;
                case "java.lang.Integer" -> Integer.valueOf(v);
                case "java.lang.Long" -> Long.valueOf(v);
                case "java.lang.Short" -> Short.valueOf(v);
                case "java.lang.Double" -> Double.valueOf(v);
                case "java.lang.Float" -> Float.valueOf(v);
                case "java.math.BigDecimal" -> new BigDecimal(v);
                case "java.lang.Boolean" -> Boolean.valueOf(v) || "Y".equalsIgnoreCase(v) || "1".equals(v);
                case "java.util.Date", "java.sql.Date" -> java.sql.Date.valueOf(LocalDate.parse(v.substring(0, Math.min(10, v.length()))));
                case "java.time.LocalDate" -> LocalDate.parse(v);
                case "java.sql.Timestamp" -> Timestamp.valueOf(parseDateTime(v));
                case "java.time.LocalDateTime" -> parseDateTime(v);
                case "java.util.List", "java.util.Collection", "java.util.Set" -> splitList(v);
                default -> fromStringConstructor(name, v, javaType);
            };
        } catch (IllegalArgumentException | DateTimeParseException e) { // includes NumberFormatException
            throw new BusinessException("INVALID_PARAMETER", "Invalid value for parameter '" + name + "': " + value);
        }
    }

    /**
     * Custom parameter types (e.g. com.sol.jasper.params.BranchList) are built from the submitted text through
     * their public (String) constructor, the same one Jaspersoft Studio uses.
     */
    private static Object fromStringConstructor(String name, String v, String javaType) {
        Constructor<?> constructor;
        try {
            Class<?> type = Class.forName(javaType, true, Thread.currentThread().getContextClassLoader());
            constructor = type.getConstructor(String.class);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            throw new BusinessException("UNSUPPORTED_PARAMETER_TYPE",
                    "Parameter '" + name + "' has unsupported type " + javaType);
        }
        try {
            return constructor.newInstance(v);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new BusinessException("INVALID_PARAMETER", "Invalid value for parameter '" + name + "': " + v);
        } catch (ReflectiveOperationException e) {
            throw new BusinessException("UNSUPPORTED_PARAMETER_TYPE",
                    "Parameter '" + name + "' has unsupported type " + javaType);
        }
    }

    private static LocalDateTime parseDateTime(String v) {
        return LocalDateTime.parse(v.replace(' ', 'T'));
    }

    private static List<String> splitList(String v) {
        return Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
