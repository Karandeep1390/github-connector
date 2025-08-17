package com.connector.github_activity_connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubUser(
        Long id,
        String login,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("html_url") String htmlUrl,
        String type,
        String name,
        String company,
        String location,
        String email,
        @JsonProperty("public_repos") Integer publicRepos,
        @JsonProperty("public_gists") Integer publicGists,
        Integer followers,
        Integer following) {
}
