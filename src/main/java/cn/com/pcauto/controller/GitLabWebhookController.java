package cn.com.pcauto.controller;

import cn.com.pcauto.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/gitlab")
@Slf4j
public class GitLabWebhookController {

    // 本机ip： 10.4.41.78
    //   http://10.4.41.78:8080/api/gitlab/webhook

    // 在 GitLab 后台配置 Webhook 时设置的 Secret Token
    private static final String SECRET_TOKEN = "123456";

    @Autowired
    private WebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleGitLabWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Gitlab-Token") String gitlabToken,
            @RequestHeader("X-Gitlab-Event") String eventType) {

        log.info("收到 GitLab Webhook 事件: {}, Payload长度: {}", eventType, payload.length());

        // 1. 基础安全校验：验证 Secret Token
        if (SECRET_TOKEN != null && !SECRET_TOKEN.equals(gitlabToken)) {
            log.warn("非法的 Webhook 请求，Token 校验失败！");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid Token");
        }

        // 2. 进阶安全校验（推荐）：验证 X-Hub-Signature-256 签名
        // if (!isValidSignature(payload, signatureHeader)) { ... }

        // 3. 异步处理业务逻辑（避免阻塞导致 GitLab 认为超时）
        // 实际开发中建议将 payload 发送到消息队列（如 RabbitMQ/Kafka）或交给 @Async 方法处理
        try {
            webhookService.processPayload(eventType, payload);
        } catch (Exception e) {
            log.error("处理 Webhook 业务逻辑异常", e);
            // 即使业务处理失败，也先返回 200 给 GitLab，避免它重复重试
        }

        // 4. 立即返回 200 OK，告诉 GitLab 已成功接收
        return ResponseEntity.ok("Webhook received successfully");
    }

    /**
     * 简单的 HMAC SHA256 签名校验示例（如果启用了签名验证）
     */
    private boolean isValidSignature(String payload, String signatureHeader) {
        try {
            String algorithm = "HmacSHA256";
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_TOKEN.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // 将计算出的哈希值与 Header 中的签名进行比对（此处省略具体的十六进制转换和比对逻辑）
            // return calculatedHex.equals(signatureHeader.replace("sha256=", ""));
            return true; 
        } catch (Exception e) {
            return false;
        }
    }
}