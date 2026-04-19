package com.metrohub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "textExtractionExecutor")
    public Executor textExtractionExecutor() {
        
        log.info("🔧 Configuring text extraction thread pool...");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core thread pool size (always available)
        executor.setCorePoolSize(2);
        
        // Maximum thread pool size (during high load)
        executor.setMaxPoolSize(5);
        
        // Queue capacity for pending tasks
        executor.setQueueCapacity(100);
        
        // Thread name prefix (useful for debugging)
        executor.setThreadNamePrefix("TextExtract-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum wait time on shutdown (30 seconds)
        executor.setAwaitTerminationSeconds(30);
        
        // Initialize the executor
        executor.initialize();
        
        log.info("✅ Text extraction thread pool configured");
        
        return executor;
    }

    @Bean(name = "nlpProcessingExecutor")
    public Executor nlpProcessingExecutor() {
        
        log.info("🔧 Configuring NLP processing thread pool...");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core thread pool size
        executor.setCorePoolSize(2);
        
        // Maximum thread pool size
        executor.setMaxPoolSize(4);
        
        // Queue capacity for pending NLP tasks
        executor.setQueueCapacity(50);
        
        // Thread name prefix
        executor.setThreadNamePrefix("NLP-Process-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum wait time on shutdown
        executor.setAwaitTerminationSeconds(30);
        
        // Initialize the executor
        executor.initialize();
        
        log.info("✅ NLP processing thread pool configured");
        
        return executor;
    }

    @Bean(name = "alertScheduler")
    public ThreadPoolTaskScheduler alertScheduler() {
        
        log.info("🔧 Configuring alert scheduler thread pool...");
        
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // Pool size for scheduled tasks
        scheduler.setPoolSize(2);
        
        // Thread name prefix
        scheduler.setThreadNamePrefix("Alert-Scheduler-");
        
        // Wait for tasks to complete on shutdown
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum wait time on shutdown
        scheduler.setAwaitTerminationSeconds(30);
        
        // Error handler
        scheduler.setErrorHandler(this::handleSchedulerError);

        // Initialize
        scheduler.initialize();

        log.info("✅ Alert scheduler thread pool configured");

        return scheduler;
    }

    private void handleSchedulerError(Throwable throwable) {
        log.error("❌ Alert scheduler error: {}", throwable.getMessage(), throwable);
    }
}