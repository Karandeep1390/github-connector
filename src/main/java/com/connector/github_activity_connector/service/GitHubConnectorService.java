package com.connector.github_activity_connector.service;

import com.connector.github_activity_connector.config.GitHubProperties;
import com.connector.github_activity_connector.dto.ConnectorRequest;
import com.connector.github_activity_connector.exception.GitHubConnectorException;
import com.connector.github_activity_connector.model.*;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class GitHubConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubConnectorService.class);

    private final WebClient webClient;
    private final GitHubProperties properties;

    public GitHubConnectorService(GitHubProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.apiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .defaultHeader(HttpHeaders.USER_AGENT, "GitHub-Activity-Connector/1.0")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .build();
    }

    @Retry(name = "github-api")
    public Mono<RepositoryActivityResponse> fetchUserRepositoryActivity(ConnectorRequest request) {
        logger.info("Fetching repository activity for user: {}", request.username());

        return fetchAllRepositories(request.username(), request.pageSize())
                .doOnSubscribe(subscription -> logger.debug("Starting repository fetch for: {}", request.username()))
                .flatMap(repo ->
                                fetchRecentCommits(repo, request.commitLimit())
                                        .collectList()
                                        .timeout(Duration.ofSeconds(10)) // per-repo timeout
                                        .onErrorResume(err -> {
                                            logger.warn("Skipping commits for repo {} due to error: {}", repo.fullName(), err.getMessage());
                                            return Mono.just(Collections.emptyList());
                                        })
                                        .map(commits -> new GithubRepository(
                                                repo.id(), repo.name(), repo.fullName(), repo.description(),
                                                repo.htmlUrl(), repo.cloneUrl(), repo.createdAt(), repo.updatedAt(),
                                                repo.pushedAt(), repo.size(), repo.stargazersCount(), repo.forksCount(),
                                                repo.language(), repo.defaultBranch(), commits
                                        )),5)
                .collectList()
                .map(repositories -> new RepositoryActivityResponse(
                        request.username(),
                        LocalDateTime.now(),
                        repositories.size(),
                        repositories,
                        extractRateLimitInfo()
                ))
                .timeout(Duration.ofMinutes(10)) // global timeout
                .doOnSuccess(response -> logger.info("Successfully fetched {} repositories for user: {}",
                        response.totalRepositories(), request.username()))
                .doOnError(error -> logger.error("Failed to fetch repository activity for user: {}",
                        request.username(), error))
                .onErrorMap(this::mapWebClientException);
    }

    private Flux<GithubRepository> fetchAllRepositories(String username, int pageSize) {
        return Flux.range(1, Integer.MAX_VALUE)
                .concatMap(page -> fetchRepositoriesPage(username, page, pageSize))
                .takeUntil(repos -> repos.size() < pageSize) // stop at last page
                .flatMapIterable(repos -> repos);
    }

    private Mono<List<GithubRepository>> fetchRepositoriesPage(String username, int page, int pageSize) {
        return webClient.get()
                .uri("/users/{username}/repos?page={page}&per_page={pageSize}&sort=updated",
                        username, page, pageSize)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GithubRepository>>() {})
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .onErrorMap(this::mapWebClientException);
    }

    // Commit pagination
    private Flux<GitHubCommit> fetchRecentCommits(GithubRepository repository, int commitLimit) {
        String[] parts = repository.fullName().split("/");
        if (parts.length != 2) {
            logger.warn("Invalid repository full name format: {}", repository.fullName());
            return Flux.empty();
        }

        String owner = parts[0];
        String repo = parts[1];
        int pageSize = 50; // GitHub default; can be made configurable

        return Flux.range(1, Integer.MAX_VALUE)
                .concatMap(page -> fetchCommitsPage(owner, repo, page, pageSize))
                .take(commitLimit) // stop once limit reached
                .doOnSubscribe(s -> logger.debug("Fetching commits for repo {} with limit {}", repository.fullName(), commitLimit));
    }

    private Flux<GitHubCommit> fetchCommitsPage(String owner, String repo, int page, int pageSize) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/commits?per_page={pageSize}&page={page}",
                        owner, repo, pageSize, page)
                .retrieve()
                .bodyToFlux(GitHubCommit.class)
                .doOnSubscribe(s -> logger.debug("Fetching commits for {}/{} page {}", owner, repo, page))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                        // Repository is empty
                        logger.info("Repository {}/{} is empty, no commits available", owner, repo);
                        return Flux.empty();
                    }
                    return Flux.error(mapWebClientException(ex));
                });
    }

    private RepositoryActivityResponse.RateLimitInfo extractRateLimitInfo() {
        // In a real implementation, you'd extract this from response headers
        return new RepositoryActivityResponse.RateLimitInfo(5000, 5000,
                LocalDateTime.now().plusHours(1));
    }

    private Throwable mapWebClientException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();

            if (statusCode.equals(HttpStatus.UNAUTHORIZED)) {
                return new GitHubConnectorException(
                        GitHubConnectorException.ErrorCode.AUTHENTICATION_FAILED,
                        "GitHub authentication failed. Please check your access token.");
            } else if (statusCode.equals(HttpStatus.FORBIDDEN)) {
                return new GitHubConnectorException(
                        GitHubConnectorException.ErrorCode.REPOSITORY_ACCESS_DENIED,
                        "Access denied. You may have hit rate limits or lack permissions.");
            } else if (statusCode.equals(HttpStatus.NOT_FOUND)) {
                return new GitHubConnectorException(
                        GitHubConnectorException.ErrorCode.USER_NOT_FOUND,
                        "User not found on GitHub.");
            } else if (statusCode.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                return new GitHubConnectorException(
                        GitHubConnectorException.ErrorCode.RATE_LIMIT_EXCEEDED,
                        "GitHub API rate limit exceeded. Please try again later.");
            } else {
                return new GitHubConnectorException(
                        GitHubConnectorException.ErrorCode.NETWORK_ERROR,
                        "GitHub API request failed: " + ex.getMessage());
            }
        }
        return new GitHubConnectorException(
                GitHubConnectorException.ErrorCode.NETWORK_ERROR,
                "Network error occurred: " + throwable.getMessage(), throwable);
    }
}
