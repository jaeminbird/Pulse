package com.peraton.cicd.dto;

import com.peraton.cicd.model.Build;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildDto {

    private Long id;
    private Long repositoryId;
    private String repositoryName;
    private String status;
    private String commitSha;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long githubRunId;
    private String workflowName;
    private String branch;
    private String commitMessage;
    private String authorName;
    private String htmlUrl;

    public static BuildDto fromEntity(Build build) {
        return BuildDto.builder()
                .id(build.getId())
                .repositoryId(build.getRepository() != null ? build.getRepository().getId() : null)
                .repositoryName(build.getRepository() != null ? build.getRepository().getName() : null)
                .status(build.getStatus() != null ? build.getStatus().name() : null)
                .commitSha(build.getCommitSha())
                .startedAt(build.getStartedAt())
                .completedAt(build.getCompletedAt())
                .githubRunId(build.getGithubRunId())
                .workflowName(build.getWorkflowName())
                .branch(build.getBranch())
                .commitMessage(build.getCommitMessage())
                .authorName(build.getAuthorName())
                .htmlUrl(build.getHtmlUrl())
                .build();
    }

    public Build toEntity() {
        Build build = new Build();
        build.setId(this.id);
        build.setCommitSha(this.commitSha);
        build.setStartedAt(this.startedAt);
        build.setCompletedAt(this.completedAt);
        build.setWorkflowName(this.workflowName);
        build.setBranch(this.branch);
        build.setCommitMessage(this.commitMessage);
        build.setAuthorName(this.authorName);
        build.setHtmlUrl(this.htmlUrl);

        if (this.status != null) {
            build.setStatus(Build.BuildStatus.valueOf(this.status));
        }

        return build;
    }
}
