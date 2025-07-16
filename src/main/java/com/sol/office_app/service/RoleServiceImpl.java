package com.sol.office_app.service;

import com.sol.office_app.dto.RoleDTO;
import com.sol.office_app.entity.Role;
import com.sol.office_app.mapper.RoleDTOMapper;
import com.sol.office_app.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleDTOMapper roleDTOMapper;

    private final RoleRepository ruleRepository;

    public RoleServiceImpl(RoleDTOMapper roleDTOMapper, RoleRepository ruleRepository) {
        this.roleDTOMapper = roleDTOMapper;
        this.ruleRepository = ruleRepository;
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
    public Optional<RoleDTO> save(RoleDTO entity) {
        Role rule = new Role();
        rule.setName(entity.name());
        rule.setDescription(entity.description());
        rule = ruleRepository.save(rule);
        return Optional.of(roleDTOMapper.apply(rule));
    }

    @Override
    public Optional<RoleDTO> update(Long id, RoleDTO entity) {
        Optional<Role> rule = ruleRepository.findById(id);
        if (rule.isPresent()) {
            Role existingRule = rule.get();
            existingRule.setName(entity.name());
            existingRule.setDescription(entity.description());
            ruleRepository.save(existingRule);
            return Optional.of(roleDTOMapper.apply(existingRule));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RoleDTO> delete(Long id, RoleDTO entity) {
        Optional<Role> existingRuleOpt = ruleRepository.findById(id);
        if (existingRuleOpt.isPresent()) {
            ruleRepository.deleteById(id);
            return Optional.of(roleDTOMapper.apply(existingRuleOpt.get()));
        } else {
            return Optional.empty();
        }
    }
}
