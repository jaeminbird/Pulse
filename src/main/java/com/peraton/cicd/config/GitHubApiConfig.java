package com.peraton.cicd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "github.api")
@Data
public class GitHubApiConfig {

    private String baseUrl = "https://api.github.com";
    private String token;
    private Integer timeout = 10000;

    /** True when a usable token is configured (not blank and not the example placeholder). */
    public boolean hasToken() {
        return token != null && !token.isBlank() && !token.startsWith("your_");
    }
}
