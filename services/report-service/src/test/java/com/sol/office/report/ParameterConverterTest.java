package com.sol.office.report;

import com.sol.office.common.exception.BusinessException;
import com.sol.jasper.params.BranchList;
import com.sol.office.report.engine.ParameterConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterConverterTest {

    @Test
    void convertsDeclaredTypes() {
        assertThat(ParameterConverter.convert("n", "42", "java.lang.Integer")).isEqualTo(42);
        assertThat(ParameterConverter.convert("n", "12.50", "java.math.BigDecimal")).isEqualTo(new BigDecimal("12.50"));
        assertThat(ParameterConverter.convert("d", "2026-10-15", "java.util.Date")).isEqualTo(java.sql.Date.valueOf("2026-10-15"));
        assertThat(ParameterConverter.convert("d", "2026-10-15", "java.time.LocalDate")).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(ParameterConverter.convert("t", "2026-10-15T08:30", "java.sql.Timestamp"))
                .isEqualTo(Timestamp.valueOf("2026-10-15 08:30:00"));
        assertThat(ParameterConverter.convert("b", "Y", "java.lang.Boolean")).isEqualTo(true);
        assertThat(ParameterConverter.convert("l", "001, 002,,003", "java.util.List")).isEqualTo(List.of("001", "002", "003"));
        assertThat(ParameterConverter.convert("s", "  ", "java.lang.String")).isNull();
    }

    @Test
    void rejectsInvalidValuesWithTheParameterName() {
        assertThatThrownBy(() -> ParameterConverter.convert("Эхлэх огноо", "15/10/2026", "java.util.Date"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Эхлэх огноо");
        assertThatThrownBy(() -> ParameterConverter.convert("n", "abc", "java.lang.Long"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void buildsCustomTypesThroughTheirStringConstructor() {
        assertThat(ParameterConverter.convert("branchId", "001, 002", "com.sol.jasper.params.BranchList"))
                .isEqualTo(new BranchList("001, 002"));
        assertThatThrownBy(() -> ParameterConverter.convert("branchId", "00'1", "com.sol.jasper.params.BranchList"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid value for parameter 'branchId'");
        assertThatThrownBy(() -> ParameterConverter.convert("x", "1", "com.example.Missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unsupported type");
    }

    @Test
    void mapsInputTypes() {
        assertThat(ParameterConverter.inputTypeFor("java.util.Date")).isEqualTo("date");
        assertThat(ParameterConverter.inputTypeFor("java.math.BigDecimal")).isEqualTo("number");
        assertThat(ParameterConverter.inputTypeFor("java.lang.String")).isEqualTo("text");
        assertThat(ParameterConverter.inputTypeFor("com.sol.jasper.params.BranchList")).isEqualTo("BranchList");
    }
}
