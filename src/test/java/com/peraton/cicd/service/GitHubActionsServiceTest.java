package com.peraton.cicd.service;

import com.peraton.cicd.model.Build.BuildStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubActionsServiceTest {

    private final GitHubActionsService service = new GitHubActionsService(null, null, null);

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "queued, null, PENDING",
            "waiting, null, PENDING",
            "in_progress, null, IN_PROGRESS",
            "completed, success, SUCCESS",
            "completed, neutral, SUCCESS",
            "completed, failure, FAILED",
            "completed, timed_out, FAILED",
            "completed, startup_failure, FAILED",
            "completed, cancelled, CANCELLED",
            "completed, skipped, CANCELLED",
            "completed, action_required, PENDING"
    })
    void mapsGitHubStatusAndConclusion(String status, String conclusion, BuildStatus expected) {
        assertThat(service.mapGitHubStatusToBuildStatus(status, conclusion)).isEqualTo(expected);
    }
}
