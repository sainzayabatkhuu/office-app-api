package com.sol.office_app.mapper;

import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.dto.SecurityRuleDTO;
import com.sol.office_app.entity.SecurityRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class SecurityRuleDTOMapper implements Function<SecurityRule, SecurityRuleDTO> {
    @Autowired
    private PrivilegeDTOMapper privilegeDTOMapper;

    @Override
    public SecurityRuleDTO apply(SecurityRule securityRule) {
        PrivilegeDTO privilegeDTO= privilegeDTOMapper.apply(securityRule.getAuthority());

        return new SecurityRuleDTO(securityRule.getId(),
                privilegeDTO,
                securityRule.getHttpMethod(),
                securityRule.getUrlPattern());
    }
}
