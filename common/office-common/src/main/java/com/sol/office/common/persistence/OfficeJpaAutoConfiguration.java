package com.sol.office.common.persistence;

import com.sol.office.common.security.SecurityUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnClass(EnableJpaAuditing.class)
@EnableJpaAuditing(auditorAwareRef = "officeAuditorAware")
public class OfficeJpaAutoConfiguration {

    public static final String SYSTEM_AUDITOR = "system";

    @Bean
    @ConditionalOnMissingBean(name = "officeAuditorAware")
    public AuditorAware<String> officeAuditorAware() {
        return () -> Optional.of(SecurityUtils.currentUsername().orElse(SYSTEM_AUDITOR));
    }
}
