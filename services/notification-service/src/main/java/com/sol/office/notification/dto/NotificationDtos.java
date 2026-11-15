package com.sol.office.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            String level,
            String category,
            String title,
            String message,
            String link,
            Map<String, Object> data,
            String source,
            boolean read,
            Instant readAt,
            Instant createdAt) {
    }

    public record UnreadCount(long count) {
    }

    /** Manual message from a user with NOTIFICATION_SEND (e.g. an announcement). */
    public record SendRequest(
            @NotEmpty @Size(max = 500) Set<String> recipients,
            @NotBlank @Pattern(regexp = "INFO|SUCCESS|WARNING|ERROR") String level,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 1000) String message,
            @Size(max = 500) String link) {
    }
}
