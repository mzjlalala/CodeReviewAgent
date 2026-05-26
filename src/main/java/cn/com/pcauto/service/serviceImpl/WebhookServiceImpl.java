package cn.com.pcauto.service.serviceImpl;


import cn.com.pcauto.service.MergeRequestReviewService;
import cn.com.pcauto.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final ObjectMapper objectMapper;
    private final MergeRequestReviewService mergeRequestReviewService;

   // 开启异步处理，不阻塞主线程
    public void processPayload(String eventType, String payload) {
        try {
            // 解析 JSON 字符串
            JsonNode jsonNode = objectMapper.readTree(payload);

            // 根据不同的事件类型处理不同的业务
            switch (eventType) {
                case "Push Hook":
                    handlePushEvent(jsonNode);
                    break;
                case "Merge Request Hook":
                    handleMergeRequestEvent(jsonNode);
                    break;
                default:
                    log.info("暂未处理的事件类型: {}", eventType);
            }
        } catch (Exception e) {
            log.error("解析 Webhook Payload 失败", e);
        }
    }

    private void handlePushEvent(JsonNode jsonNode) {
        String ref = jsonNode.get("ref").asText(); // 例如：refs/heads/master
        String userName = jsonNode.get("user_name").asText();
        
        // 提取最后一次提交的 message
        String commitMessage = "";
        if (jsonNode.has("commits") && jsonNode.get("commits").size() > 0) {
            commitMessage = jsonNode.get("commits").get(0).get("message").asText();
        }

        log.info("检测到用户 {} 向分支 {} 推送了代码，提交信息: {}", userName, ref, commitMessage);
        // TODO: 触发后续的 CI/CD 流程、代码审查通知等业务逻辑
    }

    private void handleMergeRequestEvent(JsonNode jsonNode) {
        JsonNode objectAttributes = jsonNode.get("object_attributes");
        String action = objectAttributes.get("action").asText(); // opened, updated, merged, closed
        String title = objectAttributes.get("title").asText();
        
        log.info("检测到合并请求(MR)动作: {}, 标题: {}", action, title);
        // TODO: 处理 MR 相关的业务逻辑
    }
}