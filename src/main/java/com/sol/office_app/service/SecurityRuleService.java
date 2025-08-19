package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.SecurityRuleDTO;
import com.sol.office_app.entity.Privilege;
import com.sol.office_app.entity.SecurityRule;
import com.sol.office_app.mapper.SecurityRuleDTOMapper;
import com.sol.office_app.repository.PrivilegeRepository;
import com.sol.office_app.repository.SecurityRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SecurityRuleService implements GeneralService<SecurityRuleDTO, Long> {

    @Autowired
    private SecurityRuleDTOMapper securityRuleDTOMapper;

    @Autowired
    private SecurityRuleRepository securityRuleRepository;
    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Override
    public Page<SecurityRuleDTO> findAll(Pageable pageable) {
        Page<SecurityRule> users = securityRuleRepository.findAll(pageable);
        return users.map(securityRuleDTOMapper);
    }

    @Override
    public Page<SecurityRuleDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<SecurityRuleDTO> get(Long id) {
        return securityRuleRepository.findById(id).map(securityRuleDTOMapper);
    }

    @Override
    public Optional<SecurityRuleDTO> save(SecurityRuleDTO entity) {
        Optional<Privilege> privilege = privilegeRepository.findById(entity.getAuthority().id());
        if (privilege.isPresent()) {
            SecurityRule securityRule = new SecurityRule();
            securityRule.setHttpMethod(entity.getHttpMethod());
            securityRule.setUrlPattern(entity.getUrlPattern());
            securityRule.setAuthority(privilege.get());
            securityRule = securityRuleRepository.save(securityRule);
            return Optional.of(securityRuleDTOMapper.apply(securityRule));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SecurityRuleDTO> update(Long id, SecurityRuleDTO entity) {
        Optional<SecurityRule> securityRule = securityRuleRepository.findById(id);
        if (securityRule.isPresent()) {
            SecurityRule existingSecurityRule = securityRule.get();
            existingSecurityRule.setUrlPattern(entity.getUrlPattern());
            existingSecurityRule.setHttpMethod(entity.getHttpMethod());
            securityRuleRepository.save(existingSecurityRule);
            return Optional.of(securityRuleDTOMapper.apply(existingSecurityRule));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void delete(Long id) {
        securityRuleRepository.deleteById(id);
    }

    public List<SecurityRule> getAllSecurityRules() {
        return securityRuleRepository.findAll();
    }
}
