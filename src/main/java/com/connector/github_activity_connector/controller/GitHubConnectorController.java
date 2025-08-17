package com.connector.github_activity_connector.controller;

import com.connector.github_activity_connector.dto.ConnectorRequest;
import com.connector.github_activity_connector.model.RepositoryActivityResponse;
import com.connector.github_activity_connector.service.GitHubConnectorService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubConnectorController {

    private final GitHubConnectorService connectorService;

    public GitHubConnectorController(GitHubConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @PostMapping("/repository-activity")
    public Mono<RepositoryActivityResponse> fetchRepositoryActivity(
            @Valid @RequestBody ConnectorRequest request) {

        return connectorService.fetchUserRepositoryActivity(request);
    }

    @GetMapping("/repository-activity/{username}")
    public Mono<RepositoryActivityResponse> fetchRepositoryActivity(
            @PathVariable String username,
            @RequestParam(defaultValue = "30") int pageSize,
            @RequestParam(defaultValue = "20") int commitLimit) {

        ConnectorRequest request = new ConnectorRequest(username, pageSize, commitLimit);
        return connectorService.fetchUserRepositoryActivity(request);
    }
}