package com.maa.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorConfigTest {

    private final ExecutorConfig config = new ExecutorConfig();

    @Test
    void reviewTaskExecutorShouldHaveCorrectPoolSettings() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.reviewTaskExecutor();

        assertNotNull(executor);
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(100, executor.getQueueCapacity());
        assertEquals(60, executor.getKeepAliveSeconds());
        assertEquals("review-", executor.getThreadNamePrefix());
    }

    @Test
    void reviewTaskExecutorShouldUseCallerRunsPolicy() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.reviewTaskExecutor();

        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        assertNotNull(pool);
        assertTrue(pool.getRejectedExecutionHandler()
                instanceof ThreadPoolExecutor.CallerRunsPolicy);
    }
}
