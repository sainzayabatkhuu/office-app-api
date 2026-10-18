package com.sol.office_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class AsyncConfig {

    public static final String REPORT_EXECUTOR = "reportExecutor";

    /**
     * Bounded pool for background (B) reports. Jasper fills are heavy, so they must not run on the shared
     * ForkJoin pool or without a limit.
     */
    @Bean(REPORT_EXECUTOR)
    public ThreadPoolTaskExecutor reportExecutor(@Value("${report.async.pool-size:4}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("report-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
