package com.peraton.cicd.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owner/name pair identifying a GitHub repository.
 */
public record GitHubRepoRef(String owner, String repo) {

    private static final Pattern PATTERN = Pattern.compile(
            "^(?:(?:https?://)?(?:www\\.)?github\\.com/)?([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?/?$");

    /**
     * Parse "https://github.com/owner/repo", "github.com/owner/repo(.git)" or "owner/repo".
     *
     * @throws IllegalArgumentException if the value is not a recognizable GitHub repository
     */
    public static GitHubRepoRef parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("GitHub repository URL is required");
        }
        Matcher matcher = PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Not a valid GitHub repository: '" + value + "'. Use https://github.com/owner/repo or owner/repo");
        }
        return new GitHubRepoRef(matcher.group(1), matcher.group(2));
    }

    public String fullName() {
        return owner + "/" + repo;
    }

    public String htmlUrl() {
        return "https://github.com/" + fullName();
    }
}
