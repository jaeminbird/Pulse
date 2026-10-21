package com.peraton.cicd.controller;

import com.peraton.cicd.dto.github.WorkflowRun;
import com.peraton.cicd.dto.github.WorkflowRunsResponse;
import com.peraton.cicd.exception.GitHubApiException;
import com.peraton.cicd.model.Build;
import com.peraton.cicd.repository.BuildRepository;
import com.peraton.cicd.repository.RepositoryRepository;
import com.peraton.cicd.service.GitHubApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepositoryControllerTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private BuildRepository buildRepository;

    @MockBean
    private GitHubApiClient gitHubApiClient;

    @AfterEach
    void cleanUp() {
        buildRepository.deleteAll();
        repositoryRepository.deleteAll();
    }

    @Test
    void addingRepositorySyncsRunsAndShowsThemInSummary() throws Exception {
        givenRuns("octo", "app",
                run(2L, "in_progress", null, T0.plusMinutes(10)),
                run(1L, "completed", "failure", T0));

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\": \"https://github.com/octo/app.git\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("octo/app"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/octo/app"));

        mockMvc.perform(get("/api/repositories/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repository.name").value("octo/app"))
                .andExpect(jsonPath("$[0].latestBuild.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].latestBuild.completedAt").doesNotExist())
                .andExpect(jsonPath("$[0].latestBuild.workflowName").value("CI"))
                .andExpect(jsonPath("$[0].recentBuilds.length()").value(2))
                .andExpect(jsonPath("$[0].successRate").value(0));
    }

    @Test
    void resyncUpdatesExistingRunsInsteadOfDuplicating() throws Exception {
        givenRuns("octo", "app", run(7L, "in_progress", null, T0));
        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\": \"octo/app\"}"))
                .andExpect(status().isCreated());
        Long id = repositoryRepository.findAll().get(0).getId();

        givenRuns("octo", "app", run(7L, "completed", "success", T0));
        mockMvc.perform(post("/api/repositories/{id}/sync", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(1));

        List<Build> builds = buildRepository.findAll();
        assertThat(builds).hasSize(1);
        assertThat(builds.get(0).getStatus()).isEqualTo(Build.BuildStatus.SUCCESS);
        assertThat(builds.get(0).getCompletedAt()).isNotNull();

        // Nothing changed on GitHub, so nothing should be reported as synced
        mockMvc.perform(post("/api/repositories/{id}/sync", id))
                .andExpect(jsonPath("$.syncedCount").value(0));
    }

    @Test
    void rejectsDuplicateAndInvalidRepositories() throws Exception {
        givenRuns("octo", "app");
        String body = "{\"githubUrl\": \"octo/app\"}";
        mockMvc.perform(post("/api/repositories").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/repositories").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Repository octo/app is already being monitored"));

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\": \"not a repo\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/repositories").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repositoryMissingOnGitHubIsNotSaved() throws Exception {
        when(gitHubApiClient.getWorkflowRuns(eq("octo"), eq("missing"), isNull(), isNull(), any()))
                .thenThrow(new GitHubApiException("Not Found", 404));

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\": \"octo/missing\"}"))
                .andExpect(status().isBadRequest());

        assertThat(repositoryRepository.count()).isZero();
    }

    @Test
    void deleteRemovesRepositoryAndBuilds() throws Exception {
        givenRuns("octo", "app", run(1L, "completed", "success", T0));
        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\": \"octo/app\"}"))
                .andExpect(status().isCreated());
        Long id = repositoryRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/repositories/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/repositories/{id}", id)).andExpect(status().isNotFound());
        assertThat(buildRepository.count()).isZero();
    }

    private void givenRuns(String owner, String repo, WorkflowRun... runs) {
        WorkflowRunsResponse response = new WorkflowRunsResponse();
        response.setTotalCount(runs.length);
        response.setWorkflowRuns(List.of(runs));
        when(gitHubApiClient.getWorkflowRuns(eq(owner), eq(repo), isNull(), isNull(), any()))
                .thenReturn(response);
    }

    private static WorkflowRun run(Long id, String status, String conclusion, LocalDateTime startedAt) {
        WorkflowRun run = new WorkflowRun();
        run.setId(id);
        run.setName("CI");
        run.setHeadBranch("main");
        run.setHeadSha("sha" + id);
        run.setStatus(status);
        run.setConclusion(conclusion);
        run.setCreatedAt(startedAt);
        run.setRunStartedAt(startedAt);
        run.setUpdatedAt(startedAt.plusMinutes(5));
        run.setHtmlUrl("https://github.com/octo/app/actions/runs/" + id);
        return run;
    }
}
