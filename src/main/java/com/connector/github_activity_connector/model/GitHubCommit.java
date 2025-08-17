package com.connector.github_activity_connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record GitHubCommit(
        String sha,
        @JsonProperty("html_url") String htmlUrl,
        CommitDetails commit,
        GitHubUser author,
        GitHubUser committer
) {
    public record CommitDetails(
            String message,
            GitHubUser author,
            GitHubUser committer
    ) {}

    public record GitHubUser(
            String name,
            String email,
            LocalDateTime date
    ) {}
}
