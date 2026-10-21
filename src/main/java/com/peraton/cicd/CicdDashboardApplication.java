package com.peraton.cicd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class CicdDashboardApplication {

    public static void main(String[] args) {
        // Timestamps are stored as UTC LocalDateTimes (GitHub reports UTC), so keep
        // LocalDateTime.now() consistent with them regardless of the host's zone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CicdDashboardApplication.class, args);
    }
}
