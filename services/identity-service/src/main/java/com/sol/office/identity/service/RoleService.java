package com.sol.office.identity.service;

import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.identity.domain.Privilege;
import com.sol.office.identity.domain.Role;
import com.sol.office.identity.dto.RoleDtos.PrivilegeResponse;
import com.sol.office.identity.dto.RoleDtos.RoleRequest;
import com.sol.office.identity.dto.RoleDtos.RoleResponse;
import com.sol.office.identity.mapper.IdentityMapper;
import com.sol.office.identity.repository.PrivilegeRepository;
import com.sol.office.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final IdentityMapper mapper;

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAllByOrderByCodeAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse get(Long id) {
        return mapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public List<PrivilegeResponse> listPrivileges() {
        return privilegeRepository.findAllByOrderByModuleAscCodeAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new ConflictException("Role code already exists: " + request.code());
        }
        Role role = new Role();
        role.setCode(request.code());
        apply(role, request);
        return mapper.toResponse(roleRepository.save(role));
    }

    /** The code is immutable: other services (report permissions) store role codes. */
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = load(id);
        if (!role.getCode().equals(request.code())) {
            throw new BusinessException("ROLE_CODE_IMMUTABLE", "Role code cannot be changed");
        }
        apply(role, request);
        return mapper.toResponse(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = load(id);
        if (role.isSystemRole()) {
            throw new BusinessException("SYSTEM_ROLE", "Built-in roles cannot be deleted");
        }
        if (roleRepository.isAssignedToAnyUser(id)) {
            throw new BusinessException("ROLE_IN_USE", "Role is assigned to users; remove it from them first");
        }
        roleRepository.delete(role);
    }

    private void apply(Role role, RoleRequest request) {
        role.setName(request.name());
        role.setDescription(request.description());
        Set<String> codes = request.privilegeCodes() == null ? Set.of() : request.privilegeCodes();
        List<Privilege> privileges = privilegeRepository.findByCodeIn(codes);
        if (privileges.size() != codes.size()) {
            Set<String> missing = new HashSet<>(codes);
            privileges.forEach(p -> missing.remove(p.getCode()));
            throw new BusinessException("UNKNOWN_PRIVILEGE", "Unknown privilege(s): " + missing);
        }
        role.setPrivileges(new LinkedHashSet<>(privileges));
    }

    private Role load(Long id) {
        return roleRepository.findWithPrivilegesById(id).orElseThrow(() -> NotFoundException.of("Role", id));
    }
}
