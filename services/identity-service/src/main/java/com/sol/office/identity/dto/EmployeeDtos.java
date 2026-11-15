package com.sol.office.identity.dto;

import com.sol.office.identity.domain.EmployeeStatus;
import com.sol.office.identity.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeRequest(
            @NotBlank @Size(max = 20) String employeeCode,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(max = 100) String firstName,
            @Size(max = 20) String registerNo,
            Gender gender,
            @Past LocalDate birthDate,
            @Email @Size(max = 150) String email,
            @Size(max = 32) String phone,
            @NotNull LocalDate hireDate,
            EmployeeStatus status,
            @NotNull Long branchId,
            Long departmentId,
            Long positionId,
            Long managerId,
            /** Required on update for optimistic locking. */
            Long version) {
    }

    public record TerminateRequest(@NotNull LocalDate terminationDate) {
    }

    public record EmployeeResponse(
            Long id,
            String employeeCode,
            String lastName,
            String firstName,
            String displayName,
            String registerNo,
            Gender gender,
            LocalDate birthDate,
            String email,
            String phone,
            LocalDate hireDate,
            LocalDate terminationDate,
            EmployeeStatus status,
            Long branchId,
            String branchCode,
            String branchName,
            Long departmentId,
            String departmentName,
            Long positionId,
            String positionName,
            Long managerId,
            Long version) {
    }
}
