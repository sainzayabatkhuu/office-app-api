package com.sol.office.identity.mapper;

import com.sol.office.identity.domain.AppUser;
import com.sol.office.identity.domain.Branch;
import com.sol.office.identity.domain.Department;
import com.sol.office.identity.domain.Employee;
import com.sol.office.identity.domain.JobPosition;
import com.sol.office.identity.domain.Privilege;
import com.sol.office.identity.domain.Role;
import com.sol.office.identity.domain.UserPreferences;
import com.sol.office.identity.dto.AuthDtos.PreferencesDto;
import com.sol.office.identity.dto.AuthDtos.ProfileResponse;
import com.sol.office.identity.dto.EmployeeDtos.EmployeeResponse;
import com.sol.office.identity.dto.OrgDtos.BranchResponse;
import com.sol.office.identity.dto.OrgDtos.DepartmentResponse;
import com.sol.office.identity.dto.OrgDtos.PositionResponse;
import com.sol.office.identity.dto.RoleDtos.PrivilegeResponse;
import com.sol.office.identity.dto.RoleDtos.RoleResponse;
import com.sol.office.identity.dto.UserDtos.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.LinkedHashSet;
import java.util.Set;

@Mapper
public interface IdentityMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentCode", source = "parent.code")
    BranchResponse toResponse(Branch branch);

    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchCode", source = "branch.code")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    DepartmentResponse toResponse(Department department);

    PositionResponse toResponse(JobPosition position);

    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchCode", source = "branch.code")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "positionId", source = "position.id")
    @Mapping(target = "positionName", source = "position.name")
    @Mapping(target = "managerId", source = "manager.id")
    EmployeeResponse toResponse(Employee employee);

    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "displayName", source = "employee.displayName")
    @Mapping(target = "branchCode", source = "employee.branch.code")
    @Mapping(target = "roles", expression = "java(user.roleCodes())")
    UserResponse toResponse(AppUser user);

    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "firstName", source = "employee.firstName")
    @Mapping(target = "lastName", source = "employee.lastName")
    @Mapping(target = "displayName", source = "employee.displayName")
    @Mapping(target = "email", source = "employee.email")
    @Mapping(target = "branchCode", source = "employee.branch.code")
    @Mapping(target = "branchName", source = "employee.branch.name")
    @Mapping(target = "departmentName", source = "employee.department.name")
    @Mapping(target = "positionName", source = "employee.position.name")
    @Mapping(target = "roles", expression = "java(user.roleCodes())")
    @Mapping(target = "privileges", expression = "java(user.privilegeCodes())")
    ProfileResponse toProfile(AppUser user);

    PreferencesDto toDto(UserPreferences preferences);

    @Mapping(target = "privileges", source = "privileges")
    RoleResponse toResponse(Role role);

    PrivilegeResponse toResponse(Privilege privilege);

    default Set<String> privilegeCodes(Set<Privilege> privileges) {
        Set<String> codes = new LinkedHashSet<>();
        privileges.forEach(p -> codes.add(p.getCode()));
        return codes;
    }
}
