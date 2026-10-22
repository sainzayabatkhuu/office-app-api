package com.sol.office_app.service;

import com.sol.office_app.exception.ApiException;


import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.entity.Privilege;
import com.sol.office_app.mapper.PrivilegeDTOMapper;
import com.sol.office_app.repository.PrivilegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PrivilegeService implements GeneralService<PrivilegeDTO, Long> {
    @Autowired
    private PrivilegeDTOMapper privilegeDTOMapper;
    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Override
    public Page<PrivilegeDTO> findAll(Pageable pageable) {
        Page<Privilege> privileges = privilegeRepository.findAll(pageable);
        return privileges.map(privilegeDTOMapper);
    }

    @Override
    public Page<PrivilegeDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<PrivilegeDTO> get(Long id) {
        return privilegeRepository.findById(id).map(privilegeDTOMapper);
    }

    @Override
    public Optional<PrivilegeDTO> save(PrivilegeDTO entity) {
        validateRules(entity.name());
        Privilege privilege = new Privilege();
        privilege.setValue(entity.name());
        privilege.setDescription(entity.description());
        privilege = privilegeRepository.save(privilege);
        return Optional.of(privilegeDTOMapper.apply(privilege));
    }

    @Override
    public Optional<PrivilegeDTO> update(Long id, PrivilegeDTO entity) {
        Optional<Privilege> privilege = privilegeRepository.findById(id);
        if (privilege.isPresent()) {
            validateRules(entity.name());
            Privilege existingPrivilege = privilege.get();
            existingPrivilege.setValue(entity.name());
            existingPrivilege.setDescription(entity.description());
            privilegeRepository.save(existingPrivilege);
            return Optional.of(privilegeDTOMapper.apply(existingPrivilege));
        } else {
            return Optional.empty();
        }
    }

    /** {@code method:/path[,method:/path]...}, path relative to /api/v1 (Ant patterns allowed). */
    private static final java.util.regex.Pattern RULE =
            java.util.regex.Pattern.compile("(get|post|put|delete):/[A-Za-z0-9_\\-*/{}.]*");

    static void validateRules(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Privilege rule is required.");
        }
        for (String entry : value.split(",")) {
            if (!RULE.matcher(entry.trim()).matches()) {
                throw ApiException.badRequest("INVALID_RULE",
                        "Invalid rule '" + entry.trim() + "'. Expected method:/path, e.g. get:/reports/*");
            }
        }
    }

    @Override
    public void delete(Long id) {
        privilegeRepository.deleteById(id);
    }
}
