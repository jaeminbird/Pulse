package com.peraton.cicd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "builds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildStatus status;

    @Column(name = "commit_sha", nullable = false)
    private String commitSha;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "github_run_id", unique = true)
    private Long githubRunId;

    @Column(name = "workflow_name")
    private String workflowName;

    private String branch;

    @Column(name = "commit_message", length = 1000)
    private String commitMessage;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "html_url", length = 500)
    private String htmlUrl;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public enum BuildStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        CANCELLED
    }
}
