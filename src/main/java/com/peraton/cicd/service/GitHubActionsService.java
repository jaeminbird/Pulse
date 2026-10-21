package com.peraton.cicd.service;

import com.peraton.cicd.dto.github.BuildStatusDto;
import com.peraton.cicd.dto.github.WorkflowRun;
import com.peraton.cicd.dto.github.WorkflowRunsResponse;
import com.peraton.cicd.exception.GitHubApiException;
import com.peraton.cicd.exception.ResourceNotFoundException;
import com.peraton.cicd.model.Build;
import com.peraton.cicd.model.Repository;
import com.peraton.cicd.repository.BuildRepository;
import com.peraton.cicd.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubActionsService {

    private final GitHubApiClient gitHubApiClient;
    private final RepositoryRepository repositoryRepository;
    private final BuildRepository buildRepository;

    /**
     * Get the latest build status for a repository
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @return BuildStatusDto with latest build information
     * @throws GitHubApiException if API call fails
     */
    public BuildStatusDto getLatestBuildStatus(String owner, String repo) {
        log.info("Fetching latest build status for {}/{}", owner, repo);

        WorkflowRunsResponse response = gitHubApiClient.getWorkflowRuns(owner, repo, null, null, 1);

        if (response.getWorkflowRuns() == null || response.getWorkflowRuns().isEmpty()) {
            log.warn("No workflow runs found for {}/{}", owner, repo);
            throw new ResourceNotFoundException("No workflow runs found for " + owner + "/" + repo);
        }

        WorkflowRun latestRun = response.getWorkflowRuns().get(0);
        return convertToDto(latestRun);
    }

    /**
     * Get the latest build status for a specific branch
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param branch Branch name
     * @return BuildStatusDto with latest build information for the branch
     * @throws GitHubApiException if API call fails
     */
    public BuildStatusDto getLatestBuildStatusForBranch(String owner, String repo, String branch) {
        log.info("Fetching latest build status for {}/{} on branch {}", owner, repo, branch);

        WorkflowRunsResponse response = gitHubApiClient.getWorkflowRuns(owner, repo, branch, null, 1);

        if (response.getWorkflowRuns() == null || response.getWorkflowRuns().isEmpty()) {
            log.warn("No workflow runs found for {}/{} on branch {}", owner, repo, branch);
            throw new ResourceNotFoundException("No workflow runs found for " + owner + "/" + repo + " on branch " + branch);
        }

        WorkflowRun latestRun = response.getWorkflowRuns().get(0);
        return convertToDto(latestRun);
    }

    /**
     * Get all workflow runs for a repository
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param perPage Number of results per page (max 100)
     * @return List of BuildStatusDto
     * @throws GitHubApiException if API call fails
     */
    public List<BuildStatusDto> getAllWorkflowRuns(String owner, String repo, Integer perPage) {
        log.info("Fetching all workflow runs for {}/{}", owner, repo);

        WorkflowRunsResponse response = gitHubApiClient.getWorkflowRuns(owner, repo, null, null, perPage);

        if (response.getWorkflowRuns() == null) {
            return List.of();
        }

        return response.getWorkflowRuns().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get workflow runs filtered by status
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param status Status filter (queued, in_progress, completed)
     * @param perPage Number of results per page
     * @return List of BuildStatusDto
     * @throws GitHubApiException if API call fails
     */
    public List<BuildStatusDto> getWorkflowRunsByStatus(String owner, String repo, String status, Integer perPage) {
        log.info("Fetching workflow runs for {}/{} with status {}", owner, repo, status);

        WorkflowRunsResponse response = gitHubApiClient.getWorkflowRuns(owner, repo, null, status, perPage);

        if (response.getWorkflowRuns() == null) {
            return List.of();
        }

        return response.getWorkflowRuns().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Sync recent workflow runs into the database for a repository.
     * New runs are inserted; runs already stored are updated so that
     * queued/in-progress builds pick up their final status.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param repositoryId Local repository ID in database
     * @return Number of builds created or changed
     */
    @Transactional
    public int syncWorkflowRunsToDatabase(String owner, String repo, Long repositoryId) {
        log.info("Syncing workflow runs for {}/{} to database", owner, repo);

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with ID: " + repositoryId));

        WorkflowRunsResponse response = gitHubApiClient.getWorkflowRuns(owner, repo, null, null, 50);

        if (response.getWorkflowRuns() == null) {
            return 0;
        }

        int syncedCount = 0;
        for (WorkflowRun run : response.getWorkflowRuns()) {
            if (run.getId() == null || run.getHeadSha() == null) {
                continue;
            }

            Build build = buildRepository.findByGithubRunId(run.getId()).orElse(null);
            if (build == null) {
                build = new Build();
                build.setGithubRunId(run.getId());
                build.setRepository(repository);
            } else if (!build.getRepository().getId().equals(repositoryId)) {
                log.warn("Workflow run {} already belongs to repository {}, skipping",
                        run.getId(), build.getRepository().getId());
                continue;
            } else if (!hasChanged(build, run)) {
                continue;
            }

            applyRun(build, run);
            buildRepository.save(build);
            syncedCount++;
        }

        log.info("Synced {} new or updated builds to database", syncedCount);
        return syncedCount;
    }

    private boolean hasChanged(Build build, WorkflowRun run) {
        return build.getStatus() != mapGitHubStatusToBuildStatus(run.getStatus(), run.getConclusion())
                || !Objects.equals(build.getCompletedAt(), completedAt(run));
    }

    private void applyRun(Build build, WorkflowRun run) {
        build.setCommitSha(run.getHeadSha());
        build.setStatus(mapGitHubStatusToBuildStatus(run.getStatus(), run.getConclusion()));
        build.setStartedAt(run.getRunStartedAt() != null ? run.getRunStartedAt() : run.getCreatedAt());
        build.setCompletedAt(completedAt(run));
        build.setWorkflowName(truncate(run.getName(), 255));
        build.setBranch(truncate(run.getHeadBranch(), 255));
        build.setHtmlUrl(truncate(run.getHtmlUrl(), 500));
        if (run.getHeadCommit() != null) {
            build.setCommitMessage(truncate(firstLine(run.getHeadCommit().getMessage()), 1000));
            if (run.getHeadCommit().getAuthor() != null) {
                build.setAuthorName(truncate(run.getHeadCommit().getAuthor().getName(), 255));
            }
        }
    }

    private LocalDateTime completedAt(WorkflowRun run) {
        boolean finished = "completed".equals(run.getStatus()) && !"action_required".equals(run.getConclusion());
        return finished ? run.getUpdatedAt() : null;
    }

    private static String firstLine(String value) {
        if (value == null) {
            return null;
        }
        int newline = value.indexOf('\n');
        return newline >= 0 ? value.substring(0, newline) : value;
    }

    private static String truncate(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * Convert WorkflowRun to BuildStatusDto
     */
    private BuildStatusDto convertToDto(WorkflowRun run) {
        return BuildStatusDto.builder()
                .runId(run.getId())
                .repositoryName(run.getRepository() != null ? run.getRepository().getName() : null)
                .branch(run.getHeadBranch())
                .commitSha(run.getHeadSha())
                .status(run.getStatus())
                .conclusion(run.getConclusion())
                .runNumber(run.getRunNumber())
                .startedAt(run.getRunStartedAt())
                .updatedAt(run.getUpdatedAt())
                .htmlUrl(run.getHtmlUrl())
                .commitMessage(run.getHeadCommit() != null ? run.getHeadCommit().getMessage() : null)
                .authorName(run.getHeadCommit() != null && run.getHeadCommit().getAuthor() != null
                        ? run.getHeadCommit().getAuthor().getName() : null)
                .build();
    }

    /**
     * Map GitHub status to Build status
     */
    Build.BuildStatus mapGitHubStatusToBuildStatus(String status, String conclusion) {
        if ("queued".equals(status) || "waiting".equals(status)
                || "requested".equals(status) || "pending".equals(status)) {
            return Build.BuildStatus.PENDING;
        } else if ("in_progress".equals(status)) {
            return Build.BuildStatus.IN_PROGRESS;
        } else if ("completed".equals(status)) {
            if ("success".equals(conclusion) || "neutral".equals(conclusion)) {
                return Build.BuildStatus.SUCCESS;
            } else if ("cancelled".equals(conclusion) || "skipped".equals(conclusion)) {
                return Build.BuildStatus.CANCELLED;
            } else if ("action_required".equals(conclusion)) {
                // e.g. a fork PR waiting for a maintainer to approve the run; it never executed
                return Build.BuildStatus.PENDING;
            } else {
                return Build.BuildStatus.FAILED;
            }
        }
        return Build.BuildStatus.PENDING;
    }
}
