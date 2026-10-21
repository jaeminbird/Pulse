package com.peraton.cicd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepositoryRequest {

    /** A GitHub URL (https://github.com/owner/repo) or an "owner/repo" shorthand. */
    @NotBlank(message = "GitHub repository URL is required")
    private String githubUrl;

    /** Optional display name; defaults to "owner/repo". */
    private String name;
}
