package com.metrohub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MetroHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetroHubApplication.class, args);
        
        // Print startup message
        System.out.println("\n" +
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║                                                              ║\n" +
            "║   🚇  METROHUB API STARTED SUCCESSFULLY!                     ║\n" +
            "║                                                              ║\n" +
            "║   📡  API Base URL: http://localhost:8080/api                ║\n" +
            "║   📚  API Docs:     http://localhost:8080/api/swagger-ui     ║\n" +
            "║                                                              ║\n" +
            "║   AI Intelligence Documentation System for Indian Metros    ║\n" +
            "║                                                              ║\n" +
            "╚══════════════════════════════════════════════════════════════╝\n"
        );
    }
}
