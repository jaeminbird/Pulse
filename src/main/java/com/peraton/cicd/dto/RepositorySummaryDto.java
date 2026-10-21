package com.peraton.cicd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A repository with its most recent builds, as shown on the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositorySummaryDto {

    private RepositoryDto repository;
    private BuildDto latestBuild;
    private List<BuildDto> recentBuilds;

    /** Percentage of completed recent builds that succeeded, or null if none completed. */
    private Integer successRate;
}
