package com.peraton.cicd.dto;

import com.peraton.cicd.model.Repository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDto {

    private Long id;
    private String name;
    private String githubUrl;
    private LocalDateTime createdAt;

    public static RepositoryDto fromEntity(Repository repository) {
        return RepositoryDto.builder()
                .id(repository.getId())
                .name(repository.getName())
                .githubUrl(repository.getGithubUrl())
                .createdAt(repository.getCreatedAt())
                .build();
    }
}
