package com.maa.controller;

import com.maa.common.dto.ResultMsg;
import com.maa.config.GitLabProperties;
import com.maa.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/gitlab/webhook")
@Slf4j
public class GitLabWebhookController {

    private final GitLabProperties gitLabProperties;
    private final ObjectMapper objectMapper;
    private final ReviewService reviewService;
    private final Executor reviewTaskExecutor;

    public GitLabWebhookController(
            GitLabProperties gitLabProperties,
            ObjectMapper objectMapper,
            ReviewService reviewService,
            @Qualifier("reviewTaskExecutor") Executor reviewTaskExecutor) {
        this.gitLabProperties = gitLabProperties;
        this.objectMapper = objectMapper;
        this.reviewService = reviewService;
        this.reviewTaskExecutor = reviewTaskExecutor;
    }

    @PostMapping("/MR-aiCodeReview")
    public ResultMsg<?> handleGitLabWebhook(
            @RequestBody String payload,
            HttpServletRequest request, HttpServletResponse response) {

        String gitlabToken = request.getHeader("X-Gitlab-Token");
        String eventType = request.getHeader("X-Gitlab-Event");

        log.info("收到 GitLab Webhook 事件: {}, Payload长度: {}", eventType, payload.length());

        // 1. 安全校验：验证 Secret Token
        String secretToken = gitLabProperties.getWebhookSecret();
        if (secretToken != null && !secretToken.isEmpty() && !secretToken.equals(gitlabToken)) {
            log.warn("非法的 Webhook 请求，Token 校验失败！");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return ResultMsg.unauthorized("Invalid Token");
        }

        // 2. 异步提交到线程池，立即返回
        if ("Merge Request Hook".equals(eventType)) {
            JsonNode payloadJson;
            try {
                payloadJson = objectMapper.readTree(payload);
            } catch (JsonProcessingException e) {
                log.error("Webhook payload JSON 解析失败", e);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return ResultMsg.badRequest("Invalid JSON payload");
            }
            try {
                reviewTaskExecutor.execute(() -> reviewService.handleMergeRequestEvent(payloadJson));
            } catch (Exception e) {
                log.error("提交审查任务失败", e);
            }
        } else {
            log.info("暂未处理的事件类型: {}", eventType);
        }

        // 3. 立即返回 200 OK，避免 GitLab 超时重试
        return ResultMsg.ok("Webhook received successfully");
    }
}
