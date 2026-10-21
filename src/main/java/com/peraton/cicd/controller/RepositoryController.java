package com.peraton.cicd.controller;

import com.peraton.cicd.dto.CreateRepositoryRequest;
import com.peraton.cicd.dto.RepositoryDto;
import com.peraton.cicd.dto.RepositorySummaryDto;
import com.peraton.cicd.dto.SyncResponse;
import com.peraton.cicd.exception.GitHubApiException;
import com.peraton.cicd.service.RepositoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Slf4j
public class RepositoryController {

    private final RepositoryService repositoryService;

    /**
     * GET /api/repositories - Get all monitored repositories
     */
    @GetMapping
    public ResponseEntity<List<RepositoryDto>> getAllRepositories() {
        return ResponseEntity.ok(repositoryService.getAllRepositories());
    }

    /**
     * GET /api/repositories/summary - Get repositories with their recent builds (dashboard view)
     */
    @GetMapping("/summary")
    public ResponseEntity<List<RepositorySummaryDto>> getSummaries() {
        return ResponseEntity.ok(repositoryService.getSummaries());
    }

    /**
     * GET /api/repositories/{id} - Get repository by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RepositoryDto> getRepository(@PathVariable Long id) {
        return ResponseEntity.ok(repositoryService.getRepository(id));
    }

    /**
     * POST /api/repositories - Start monitoring a GitHub repository.
     * An initial sync is attempted right away; if GitHub can't be reached the
     * repository is still added and the scheduler will pick it up later.
     */
    @PostMapping
    public ResponseEntity<RepositoryDto> createRepository(@Valid @RequestBody CreateRepositoryRequest request) {
        log.info("POST /api/repositories - Adding {}", request.getGithubUrl());
        RepositoryDto created = repositoryService.createRepository(request);
        try {
            repositoryService.syncRepository(created.getId());
        } catch (GitHubApiException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                repositoryService.deleteRepository(created.getId());
                throw new IllegalArgumentException("GitHub repository " + created.getName()
                        + " was not found (if it is private, configure GITHUB_TOKEN)");
            }
            log.warn("Initial sync for {} failed: {}", created.getName(), e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/repositories/{id}/sync - Sync builds for one repository from GitHub
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncResponse> syncRepository(@PathVariable Long id) {
        log.info("POST /api/repositories/{}/sync", id);
        return ResponseEntity.ok(repositoryService.syncRepository(id));
    }

    /**
     * DELETE /api/repositories/{id} - Stop monitoring a repository and remove its builds
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable Long id) {
        log.info("DELETE /api/repositories/{}", id);
        repositoryService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }
}
