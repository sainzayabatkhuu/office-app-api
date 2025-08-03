package com.sol.office_app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationMessage(String type, String title, String message, Map<String, String> data) {
}
