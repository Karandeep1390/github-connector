package com.connector.github_activity_connector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConnectorRequest(
        @NotBlank(message = "Username is required")
        String username,

        @Positive(message = "Page size must be positive")
        Integer pageSize,

        @Positive(message = "Commit limit must be positive")
        Integer commitLimit
) {
    public ConnectorRequest(String username) {
        this(username, 30, 20);
    }
}