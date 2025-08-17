package com.connector.github_activity_connector.model;

import java.time.LocalDateTime;
import java.util.List;

public record RepositoryActivityResponse(
        String username,
        LocalDateTime fetchedAt,
        Integer totalRepositories,
        List<GithubRepository> repositories,
        RateLimitInfo rateLimitInfo
) {
    public record RateLimitInfo(
            Integer remaining,
            Integer limit,
            LocalDateTime resetTime
    ) {}
}
