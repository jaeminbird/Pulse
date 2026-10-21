package com.peraton.cicd.service;

import com.peraton.cicd.dto.BuildDto;
import com.peraton.cicd.dto.CreateRepositoryRequest;
import com.peraton.cicd.dto.RepositoryDto;
import com.peraton.cicd.dto.RepositorySummaryDto;
import com.peraton.cicd.dto.SyncResponse;
import com.peraton.cicd.exception.ResourceNotFoundException;
import com.peraton.cicd.model.Build;
import com.peraton.cicd.model.Repository;
import com.peraton.cicd.repository.BuildRepository;
import com.peraton.cicd.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final BuildRepository buildRepository;
    private final GitHubActionsService gitHubActionsService;

    @Transactional(readOnly = true)
    public List<RepositoryDto> getAllRepositories() {
        return repositoryRepository.findAllOrderByCreatedAtDesc().stream()
                .map(RepositoryDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepositoryDto getRepository(Long id) {
        return RepositoryDto.fromEntity(findRepository(id));
    }

    /**
     * Get every repository together with its recent builds, for the dashboard.
     */
    @Transactional(readOnly = true)
    public List<RepositorySummaryDto> getSummaries() {
        return repositoryRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public RepositoryDto createRepository(CreateRepositoryRequest request) {
        GitHubRepoRef ref = GitHubRepoRef.parse(request.getGithubUrl());
        String name = request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim()
                : ref.fullName();

        if (repositoryRepository.existsByGithubUrl(ref.htmlUrl())) {
            throw new IllegalArgumentException("Repository " + ref.fullName() + " is already being monitored");
        }
        if (repositoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("A repository named '" + name + "' already exists");
        }

        Repository repository = new Repository();
        repository.setName(name);
        repository.setGithubUrl(ref.htmlUrl());
        Repository saved = repositoryRepository.save(repository);
        log.info("Added repository {} ({})", saved.getName(), saved.getGithubUrl());
        return RepositoryDto.fromEntity(saved);
    }

    @Transactional
    public void deleteRepository(Long id) {
        Repository repository = findRepository(id);
        repositoryRepository.delete(repository);
        log.info("Deleted repository {} and its builds", repository.getName());
    }

    /**
     * Pull the latest workflow runs from GitHub for one repository.
     */
    public SyncResponse syncRepository(Long id) {
        Repository repository = findRepository(id);
        GitHubRepoRef ref = GitHubRepoRef.parse(repository.getGithubUrl());
        int syncedCount = gitHubActionsService.syncWorkflowRunsToDatabase(ref.owner(), ref.repo(), id);
        return SyncResponse.builder()
                .success(true)
                .syncedCount(syncedCount)
                .message(String.format("Synced %d new or updated builds from GitHub", syncedCount))
                .repositoryName(repository.getName())
                .build();
    }

    private Repository findRepository(Long id) {
        return repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", id));
    }

    private RepositorySummaryDto toSummary(Repository repository) {
        List<BuildDto> recent = buildRepository.findTop10ByRepositoryIdOrderByStartedAtDesc(repository.getId())
                .stream()
                .map(BuildDto::fromEntity)
                .toList();

        long finished = recent.stream().filter(b -> isFinished(b.getStatus())).count();
        long succeeded = recent.stream().filter(b -> Build.BuildStatus.SUCCESS.name().equals(b.getStatus())).count();

        return RepositorySummaryDto.builder()
                .repository(RepositoryDto.fromEntity(repository))
                .latestBuild(recent.isEmpty() ? null : recent.get(0))
                .recentBuilds(recent)
                .successRate(finished == 0 ? null : (int) Math.round(100.0 * succeeded / finished))
                .build();
    }

    private static boolean isFinished(String status) {
        return Build.BuildStatus.SUCCESS.name().equals(status) || Build.BuildStatus.FAILED.name().equals(status);
    }
}
