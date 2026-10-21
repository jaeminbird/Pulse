package com.peraton.cicd.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate githubRestTemplate(RestTemplateBuilder builder, GitHubApiConfig gitHubApiConfig) {
        Duration timeout = Duration.ofMillis(gitHubApiConfig.getTimeout());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .additionalInterceptors(githubApiInterceptor())
                .build();
    }

    private ClientHttpRequestInterceptor githubApiInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add("Accept", "application/vnd.github.v3+json");
            request.getHeaders().add("X-GitHub-Api-Version", "2022-11-28");
            return execution.execute(request, body);
        };
    }
}
