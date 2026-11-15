package com.sol.office.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "office.notification")
public record NotificationProperties(
        @DefaultValue("90") int retentionDays,
        /* Browser origins allowed to open the WebSocket. */
        @DefaultValue("http://localhost:4300,http://localhost:3000") List<String> allowedOrigins) {
}
