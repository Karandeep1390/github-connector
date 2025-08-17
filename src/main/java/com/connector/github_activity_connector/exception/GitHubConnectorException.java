package com.connector.github_activity_connector.exception;

public class GitHubConnectorException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int httpStatus;

    public GitHubConnectorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getDefaultHttpStatus();
    }

    public GitHubConnectorException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getDefaultHttpStatus();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public enum ErrorCode {
        RATE_LIMIT_EXCEEDED(429),
        AUTHENTICATION_FAILED(401),
        USER_NOT_FOUND(404),
        REPOSITORY_ACCESS_DENIED(403),
        NETWORK_ERROR(503),
        INVALID_REQUEST(400),
        INTERNAL_ERROR(500);

        private final int defaultHttpStatus;

        ErrorCode(int defaultHttpStatus) {
            this.defaultHttpStatus = defaultHttpStatus;
        }

        public int getDefaultHttpStatus() {
            return defaultHttpStatus;
        }
    }
}