package com.debankar.featureflags.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    public Executor impressionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50000);
        executor.setThreadNamePrefix("impression-");
        executor.setRejectedExecutionHandler(
                (r, e) -> System.err.println(
                        "Impression queue full - dropping impression record"
                )
        );
        executor.initialize();
        return executor;
    }
}
