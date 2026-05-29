package com.maa.controller;

import com.maa.common.dto.ResultMsg;
import com.maa.config.GitLabProperties;
import com.maa.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/gitlab/webhook")
@Slf4j
@RequiredArgsConstructor
public class GitLabWebhookController {

    private final GitLabProperties gitLabProperties;
    private final ObjectMapper objectMapper;
    private final ReviewService reviewService;

    @PostMapping("/aiCodeReview")
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

        // 2. 路由到对应的业务处理
        try {
            if ("Merge Request Hook".equals(eventType)) {
                reviewService.handleMergeRequestEvent(objectMapper.readTree(payload));
            } else {
                log.info("暂未处理的事件类型: {}", eventType);
            }
        } catch (Exception e) {
            log.error("处理 Webhook 业务逻辑异常", e);
        }

        // 3. 立即返回 200 OK，避免 GitLab 超时重试
        return ResultMsg.ok("Webhook received successfully");
    }
}
