package com.sol.office.common.event;

import java.time.Instant;
import java.util.Map;

/**
 * A message for one user, delivered by notification-service (stored + pushed over WebSocket).
 *
 * @param recipient username of the target user
 * @param level     INFO | SUCCESS | WARNING | ERROR
 * @param link      optional front-end route or API path the notification points to
 */
public record NotificationEvent(
        String recipient,
        String level,
        String category,
        String title,
        String message,
        String link,
        Map<String, Object> data,
        String source,
        Instant occurredAt
) {
    public static NotificationEvent of(String source, String recipient, String level, String category,
                                       String title, String message, String link, Map<String, Object> data) {
        return new NotificationEvent(recipient, level, category, title, message, link,
                data == null ? Map.of() : data, source, Instant.now());
    }
}
