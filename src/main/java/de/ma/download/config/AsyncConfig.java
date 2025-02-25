package de.ma.download.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Value("${file.generation.internal.core-pool-size:5}")
    private int internalCorePoolSize;

    @Value("${file.generation.internal.max-pool-size:10}")
    private int internalMaxPoolSize;

    @Value("${file.generation.internal.queue-capacity:25}")
    private int internalQueueCapacity;

    @Value("${file.generation.external.core-pool-size:3}")
    private int externalCorePoolSize;

    @Value("${file.generation.external.max-pool-size:5}")
    private int externalMaxPoolSize;

    @Value("${file.generation.external.queue-capacity:10}")
    private int externalQueueCapacity;

    /**
     * Executor for internal file generation tasks
     */
    @Bean(name = "fileGenerationTaskExecutor")
    public TaskExecutor fileGenerationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(internalCorePoolSize);
        executor.setMaxPoolSize(internalMaxPoolSize);
        executor.setQueueCapacity(internalQueueCapacity);
        executor.setThreadNamePrefix("FileGen-");

        // Reject policy that logs discarded tasks
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        return executor;
    }

    /**
     * Executor for external service calls with different configuration
     */
    @Bean(name = "externalServiceTaskExecutor")
    public TaskExecutor externalServiceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(externalCorePoolSize);
        executor.setMaxPoolSize(externalMaxPoolSize);
        executor.setQueueCapacity(externalQueueCapacity);
        executor.setThreadNamePrefix("ExtSvc-");

        // Use thread policy that makes the caller wait
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60); // Keep threads alive longer for reuse
        executor.initialize();

        return executor;
    }

    /**
     * Configure RestTemplate with timeout settings
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
