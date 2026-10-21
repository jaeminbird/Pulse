package com.peraton.cicd.service;

import com.peraton.cicd.config.GitHubApiConfig;
import com.peraton.cicd.dto.github.WorkflowRunsResponse;
import com.peraton.cicd.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubApiClient {

    @Qualifier("githubRestTemplate")
    private final RestTemplate restTemplate;
    private final GitHubApiConfig gitHubApiConfig;

    /**
     * Fetch workflow runs for a given repository
     *
     * @param owner Repository owner (username or organization)
     * @param repo Repository name
     * @return WorkflowRunsResponse containing list of workflow runs
     * @throws GitHubApiException if API call fails
     */
    public WorkflowRunsResponse getWorkflowRuns(String owner, String repo) {
        return getWorkflowRuns(owner, repo, null, null, null);
    }

    /**
     * Fetch workflow runs for a given repository with filters
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param branch Filter by branch name
     * @param status Filter by status (queued, in_progress, completed)
     * @param perPage Number of results per page (max 100)
     * @return WorkflowRunsResponse containing list of workflow runs
     * @throws GitHubApiException if API call fails
     */
    public WorkflowRunsResponse getWorkflowRuns(String owner, String repo, String branch, String status, Integer perPage) {
        String url = buildWorkflowRunsUrl(owner, repo, branch, status, perPage);

        log.debug("Fetching workflow runs from GitHub API: {}", url);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<WorkflowRunsResponse> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    entity,
                    WorkflowRunsResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully fetched {} workflow runs", response.getBody().getTotalCount());
                return response.getBody();
            } else {
                throw new GitHubApiException("Unexpected response from GitHub API", response.getStatusCode().value());
            }

        } catch (HttpStatusCodeException e) {
            log.error("GitHub API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException(
                    "Failed to fetch workflow runs: " + e.getMessage(),
                    e.getStatusCode().value(),
                    e
            );
        } catch (RestClientException e) {
            log.error("Error calling GitHub API", e);
            throw new GitHubApiException("Failed to connect to GitHub API: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific workflow run by ID
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param runId Workflow run ID
     * @return WorkflowRun details
     * @throws GitHubApiException if API call fails
     */
    public com.peraton.cicd.dto.github.WorkflowRun getWorkflowRun(String owner, String repo, Long runId) {
        String url = UriComponentsBuilder.fromHttpUrl(gitHubApiConfig.getBaseUrl())
                .path("/repos/{owner}/{repo}/actions/runs/{runId}")
                .buildAndExpand(owner, repo, runId)
                .encode()
                .toUriString();

        log.debug("Fetching workflow run {} from GitHub API", runId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<com.peraton.cicd.dto.github.WorkflowRun> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    entity,
                    com.peraton.cicd.dto.github.WorkflowRun.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully fetched workflow run {}", runId);
                return response.getBody();
            } else {
                throw new GitHubApiException("Unexpected response from GitHub API", response.getStatusCode().value());
            }

        } catch (HttpStatusCodeException e) {
            log.error("GitHub API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException(
                    "Failed to fetch workflow run: " + e.getMessage(),
                    e.getStatusCode().value(),
                    e
            );
        } catch (RestClientException e) {
            log.error("Error calling GitHub API", e);
            throw new GitHubApiException("Failed to connect to GitHub API: " + e.getMessage(), e);
        }
    }

    private String buildWorkflowRunsUrl(String owner, String repo, String branch, String status, Integer perPage) {
        UriComponentsBuilder url = UriComponentsBuilder.fromHttpUrl(gitHubApiConfig.getBaseUrl())
                .path("/repos/{owner}/{repo}/actions/runs");

        if (branch != null && !branch.isEmpty()) {
            url.queryParam("branch", branch);
        }
        if (status != null && !status.isEmpty()) {
            url.queryParam("status", status);
        }
        if (perPage != null && perPage > 0) {
            url.queryParam("per_page", Math.min(perPage, 100));
        }

        return url.buildAndExpand(owner, repo).encode().toUriString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Public repositories work without a token (at a lower rate limit), so only
        // send credentials when a real token has been configured.
        if (gitHubApiConfig.hasToken()) {
            headers.setBearerAuth(gitHubApiConfig.getToken());
        }
        return headers;
    }
}
