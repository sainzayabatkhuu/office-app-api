package com.sol.office_app.service;

import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.entity.Privilege;
import com.sol.office_app.exception.ApiException;
import com.sol.office_app.repository.PrivilegeRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.RoleDTO;
import com.sol.office_app.entity.Role;
import com.sol.office_app.mapper.RoleDTOMapper;
import com.sol.office_app.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService implements GeneralService<RoleDTO, Long> {

    private final RoleDTOMapper roleDTOMapper;

    private final RoleRepository ruleRepository;

    private final PrivilegeRepository privilegeRepository;

    public RoleService(RoleDTOMapper roleDTOMapper, RoleRepository ruleRepository, PrivilegeRepository privilegeRepository) {
        this.roleDTOMapper = roleDTOMapper;
        this.ruleRepository = ruleRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public Page<RoleDTO> findAll(Pageable pageable) {
        Page<Role> rules = ruleRepository.findAll(pageable);
        return rules.map(roleDTOMapper);
    }

    @Override
    public Page<RoleDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<RoleDTO> get(Long id) {
        return ruleRepository.findById(id).map(roleDTOMapper);
    }

    @Override
    @Transactional
    public Optional<RoleDTO> save(RoleDTO entity) {
        validateName(entity.name());
        if (ruleRepository.findByName(entity.name().trim()).isPresent()) {
            throw ApiException.conflict("Role already exists: " + entity.name());
        }
        Role rule = new Role();
        rule.setName(entity.name().trim());
        rule.setDescription(entity.description());
        rule.setPrivileges(resolvePrivileges(entity.privileges()));
        rule = ruleRepository.save(rule);
        return Optional.of(roleDTOMapper.apply(rule));
    }

    @Override
    /**
     * Updates name, description and — when {@code privileges} is sent — the role's privileges.
     * Users holding the role get the new privileges with their next token (at most one access-token lifetime).
     */
    @Transactional
    public Optional<RoleDTO> update(Long id, RoleDTO entity) {
        Optional<Role> rule = ruleRepository.findById(id);
        if (rule.isPresent()) {
            Role existingRule = rule.get();
            validateName(entity.name());
            ruleRepository.findByName(entity.name().trim())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw ApiException.conflict("Role already exists: " + entity.name()); });
            existingRule.setName(entity.name().trim());
            existingRule.setDescription(entity.description());
            if (entity.privileges() != null) {
                existingRule.setPrivileges(resolvePrivileges(entity.privileges()));
            }
            ruleRepository.save(existingRule);
            return Optional.of(roleDTOMapper.apply(existingRule));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void delete(Long id) {
        ruleRepository.deleteById(id);
    }

    private static void validateName(String name) {
        if (name == null || !name.trim().matches("[A-Z][A-Z0-9_]{1,49}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Role name must be UPPER_CASE letters, digits and _ (e.g. ROLE_TELLER).");
        }
    }

    /** Privileges by id; unknown ids are an error rather than silently dropped. */
    private List<Privilege> resolvePrivileges(Collection<PrivilegeDTO> requested) {
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = requested.stream().map(PrivilegeDTO::id).filter(Objects::nonNull).distinct().toList();
        List<Privilege> found = privilegeRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw ApiException.badRequest("UNKNOWN_PRIVILEGE", "One or more privileges do not exist.");
        }
        return new ArrayList<>(found);
    }
}
