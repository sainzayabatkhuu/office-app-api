package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.SecurityRuleDTO;
import com.sol.office_app.entity.SecurityRule;

import java.util.List;

public interface SecurityRuleService extends GeneralService<SecurityRuleDTO, Long> {
    List<SecurityRule> getAllSecurityRules();
}
