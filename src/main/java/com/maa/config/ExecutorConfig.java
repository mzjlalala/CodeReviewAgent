package com.maa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Webhook 回调异步线程池。
 * <p>
 * Controller 收到 Webhook 后通过
 * {@code reviewTaskExecutor.execute(() -> reviewService.handle(...))} 提交任务到该线程池执行，
 * 避免长时间阻塞 Servlet 请求线程。
 * <p>
 * 注意：当线程池 + 队列全满时，{@code CallerRunsPolicy} 会让 Controller
 * 线程自行执行审查逻辑，此时 HTTP 响应会延迟几十秒。这是有意的背压设计，
 * 避免任务被静默丢弃。
 */
@Configuration
public class ExecutorConfig {

    @Bean
    public Executor reviewTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：常态下保持 2 个线程待命
        executor.setCorePoolSize(2);
        // 最大线程数：突发流量时最多扩容到 4 个
        executor.setMaxPoolSize(4);
        // 队列容量：核心线程忙时，任务先入队缓冲
        executor.setQueueCapacity(100);
        // 空闲线程存活时间（秒），超过该时间未使用的非核心线程会被回收
        executor.setKeepAliveSeconds(60);
        // 线程名前缀，方便日志和监控中识别
        executor.setThreadNamePrefix("review-");
        // 拒绝策略：线程池 + 队列都满时，由调用线程（Controller）自行执行，
        // 提供自然背压，避免丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 应用关闭时等待队列中的任务执行完毕
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待 30 秒后强制终止
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
