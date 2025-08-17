package com.connector.github_activity_connector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "github")
@Validated
public record GitHubProperties(
        @NotBlank String apiUrl,
        @NotBlank String token,
        @Positive int defaultPageSize,
        @Positive int maxRetries,
        @Positive long retryDelayMs,
        @Positive int timeoutSeconds) {
}
