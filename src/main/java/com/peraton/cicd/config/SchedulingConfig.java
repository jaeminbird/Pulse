package com.peraton.cicd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "scheduler")
@Data
public class SchedulingConfig {

    private boolean enabled = true;
    private long githubSyncRate = 300000; // Milliseconds between syncs (5 minutes)
    private List<MonitoredRepository> repositories = new ArrayList<>();

    @Data
    public static class MonitoredRepository {
        private Long id;
        private String owner;
        private String repo;
        private boolean enabled = true;
    }
}
