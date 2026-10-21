package com.peraton.cicd.service;

import com.peraton.cicd.config.SchedulingConfig;
import com.peraton.cicd.exception.GitHubApiException;
import com.peraton.cicd.model.Repository;
import com.peraton.cicd.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    // Guards against a manual trigger overlapping a scheduled run
    private final ReentrantLock syncLock = new ReentrantLock();

    private final GitHubActionsService gitHubActionsService;
    private final RepositoryRepository repositoryRepository;
    private final SchedulingConfig schedulingConfig;

    /**
     * Scheduled task that polls GitHub API every 5 minutes
     * and saves new builds to the database
     */
    @Scheduled(fixedRateString = "${scheduler.github-sync-rate:300000}") // Default: 5 minutes (300000ms)
    public void syncGitHubBuildsScheduled() {
        if (!schedulingConfig.isEnabled()) {
            log.debug("Scheduler is disabled, skipping GitHub sync");
            return;
        }
        runSync();
    }

    private void runSync() {
        if (!syncLock.tryLock()) {
            log.info("A GitHub sync is already running, skipping");
            return;
        }

        log.info("========================================");
        log.info("Starting scheduled GitHub builds sync at {}", LocalDateTime.now());
        log.info("========================================");

        int totalSynced = 0;
        int successCount = 0;
        int failureCount = 0;

        try {
            // Get configured repositories from application.yml
            List<SchedulingConfig.MonitoredRepository> configuredRepos = schedulingConfig.getRepositories();

            if (configuredRepos.isEmpty()) {
                syncAllRepositories();
                return;
            }

            // Sync configured repositories
            for (SchedulingConfig.MonitoredRepository monitoredRepo : configuredRepos) {
                if (!monitoredRepo.isEnabled()) {
                    log.debug("Skipping disabled repository: {}/{}", monitoredRepo.getOwner(), monitoredRepo.getRepo());
                    continue;
                }

                try {
                    log.info("Syncing repository: {}/{} (ID: {})",
                            monitoredRepo.getOwner(), monitoredRepo.getRepo(), monitoredRepo.getId());

                    int syncedCount = gitHubActionsService.syncWorkflowRunsToDatabase(
                            monitoredRepo.getOwner(),
                            monitoredRepo.getRepo(),
                            monitoredRepo.getId()
                    );

                    totalSynced += syncedCount;
                    successCount++;

                    log.info("Successfully synced {} new builds for {}/{}",
                            syncedCount, monitoredRepo.getOwner(), monitoredRepo.getRepo());

                } catch (GitHubApiException e) {
                    failureCount++;
                    log.error("GitHub API error while syncing {}/{}: {}",
                            monitoredRepo.getOwner(), monitoredRepo.getRepo(), e.getMessage());
                } catch (IllegalArgumentException e) {
                    failureCount++;
                    log.error("Repository configuration error for {}/{}: {}",
                            monitoredRepo.getOwner(), monitoredRepo.getRepo(), e.getMessage());
                } catch (Exception e) {
                    failureCount++;
                    log.error("Unexpected error while syncing {}/{}: {}",
                            monitoredRepo.getOwner(), monitoredRepo.getRepo(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Critical error during scheduled sync: {}", e.getMessage(), e);
        } finally {
            log.info("========================================");
            log.info("Scheduled GitHub sync completed at {}", LocalDateTime.now());
            log.info("Total builds synced: {}", totalSynced);
            log.info("Successful repositories: {}", successCount);
            log.info("Failed repositories: {}", failureCount);
            log.info("========================================");
            syncLock.unlock();
        }
    }

    /**
     * Fallback method to sync all repositories in database when no config is provided
     */
    private void syncAllRepositories() {
        log.info("Attempting to sync all repositories from database");

        List<Repository> allRepositories = repositoryRepository.findAll();

        if (allRepositories.isEmpty()) {
            log.warn("No repositories found in database. Please add repositories first.");
            return;
        }

        int totalSynced = 0;
        int successCount = 0;
        int failureCount = 0;

        for (Repository repository : allRepositories) {
            try {
                GitHubRepoRef ref = GitHubRepoRef.parse(repository.getGithubUrl());
                String owner = ref.owner();
                String repo = ref.repo();

                log.info("Syncing repository: {}/{} (ID: {})", owner, repo, repository.getId());

                int syncedCount = gitHubActionsService.syncWorkflowRunsToDatabase(
                        owner,
                        repo,
                        repository.getId()
                );

                totalSynced += syncedCount;
                successCount++;

                log.info("Successfully synced {} new builds for {}/{}", syncedCount, owner, repo);

            } catch (Exception e) {
                failureCount++;
                log.error("Error syncing repository {}: {}", repository.getName(), e.getMessage());
            }
        }

        log.info("Database repository sync completed. Synced: {}, Success: {}, Failed: {}",
                totalSynced, successCount, failureCount);
    }

    /**
     * Run a sync immediately, even if scheduled syncing is disabled.
     */
    public void triggerManualSync() {
        log.info("Manual sync triggered");
        runSync();
    }
}
