package com.connector.github_activity_connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record GithubRepository(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("clone_url") String cloneUrl,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("pushed_at") LocalDateTime pushedAt,
        Integer size,
        @JsonProperty("stargazers_count") Integer stargazersCount,
        @JsonProperty("forks_count") Integer forksCount,
        String language,
        @JsonProperty("default_branch") String defaultBranch,
        List<GitHubCommit> recentCommits
) {}
