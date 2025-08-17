package com.connector.github_activity_connector.controller;

import com.connector.github_activity_connector.exception.GitHubConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GitHubConnectorException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubConnectorException(
            GitHubConnectorException ex) {

        logger.error("GitHub connector error: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = Map.of(
                "error", ex.getErrorCode().name(),
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now(),
                "status", ex.getHttpStatus()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(BindException ex) {
        logger.error("Validation error: {}", ex.getMessage());

        Map<String, Object> errorResponse = Map.of(
                "error", "VALIDATION_ERROR",
                "message", "Invalid request parameters",
                "details", ex.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .toList(),
                "timestamp", LocalDateTime.now(),
                "status", 400
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred",
                "timestamp", LocalDateTime.now(),
                "status", 500
        );

        return ResponseEntity.internalServerError().body(errorResponse);
    }
}
