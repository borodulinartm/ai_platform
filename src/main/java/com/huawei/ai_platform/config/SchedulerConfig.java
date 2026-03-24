package com.huawei.ai_platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Settings for the scheduling configuration
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulerConfig {
    /**
     * Constructs scheduling bean
     *
     * @return Thread pool task executor bean
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        scheduler.setThreadNamePrefix("Scheduled-task-");
        scheduler.setRejectedExecutionHandler((task, executor) -> {
            log.error("Sorry, task was rejected");
        });

        scheduler.initialize();

        return scheduler;
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutorAsync() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1000);
        executor.setRejectedExecutionHandler((task, thread) -> {
            log.error("Sorry, for thread = {} task = {} was rejected", thread, task);
        });

        executor.initialize();
        return executor;
    }
}
