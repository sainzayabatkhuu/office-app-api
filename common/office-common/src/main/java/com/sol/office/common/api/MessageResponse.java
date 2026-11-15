package com.sol.office.common.api;

import java.util.Map;

/**
 * Simple acknowledgement body for commands that have no resource to return.
 */
public record MessageResponse(String message, Map<String, Object> data) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message, Map.of());
    }

    public static MessageResponse of(String message, Map<String, Object> data) {
        return new MessageResponse(message, data);
    }
}
