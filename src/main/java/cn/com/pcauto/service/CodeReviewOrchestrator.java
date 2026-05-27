package cn.com.pcauto.service;

import cn.com.pcauto.agent.CodeReviewAgent;
import cn.com.pcauto.config.CodeReviewProperties;
import cn.com.pcauto.dto.gitlab.FileChange;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
import cn.com.pcauto.review.CodeReviewPromptBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 代码审查编排器 —— 直接接收 Webhook，路由事件并编排审查流程。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeReviewOrchestrator {

    private static final Set<String> DIFF_ACTIONS = new HashSet<>(Arrays.asList(
            "open", "update", "reopen"
    ));

    private final ObjectMapper objectMapper;
    private final GitLabApiService gitLabApiService;
    private final CodeReviewAgent codeReviewAgent;
    private final CodeReviewProperties codeReviewProperties;

    // ────────── 入口 ──────────

    public void handleWebhook(String eventType, String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
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
        } catch (JsonProcessingException e) {
            log.error("解析 Webhook Payload 失败", e);
        }
    }

    // ────────── Push 事件 ──────────

    private void handlePushEvent(JsonNode jsonNode) {
        String ref = jsonNode.get("ref").asText();
        String userName = jsonNode.get("user_name").asText();
        String commitMessage = "";
        if (jsonNode.has("commits") && jsonNode.get("commits").size() > 0) {
            commitMessage = jsonNode.get("commits").get(0).get("message").asText();
        }
        log.info("Push: {} → {}, 提交: {}", userName, ref, commitMessage);
        // TODO: 触发后续 CI/CD
    }

    // ────────── MR 事件 ──────────

    private void handleMergeRequestEvent(JsonNode payload) {
        JsonNode attrs = payload.get("object_attributes");
        if (attrs == null || attrs.isNull()) {
            log.warn("MR Webhook 缺少 object_attributes，跳过");
            return;
        }
        JsonNode project = payload.get("project");
        if (project == null || project.isNull()) {
            log.warn("MR Webhook 缺少 project，跳过");
            return;
        }

        String action = textOrEmpty(attrs, "action");
        long mrIid = attrs.get("iid").asLong();
        long projectId = project.get("id").asLong();

        log.info("MR Webhook: action={}, title={}, projectId={}, mrIid={}",
                action, textOrEmpty(attrs, "title"), projectId, mrIid);

        if (!DIFF_ACTIONS.contains(action)) {
            log.info("MR 动作 {} 无需拉取 diff，跳过", action);
            return;
        }
        review(projectId, mrIid);
    }

    // ────────── 审查流程 ──────────

    private void review(long projectId, long mrIid) {
        try {
            MergeRequestChangesResponse changes =
                    gitLabApiService.getMergeRequestChanges(projectId, mrIid);

            if (changes == null || changes.getChanges() == null || changes.getChanges().isEmpty()) {
                log.info("MR !{} 无文件变更", mrIid);
                return;
            }

            logSummary(mrIid, changes);

            if (!codeReviewProperties.isEnabled()) {
                log.info("AI 审查已关闭，跳过 MR !{}", mrIid);
                return;
            }

            log.info("Agent 开始审查 MR !{}, 文件数={}", mrIid, changes.getChanges().size());
            changes.setIid(mrIid);
            changes.setProjectId(projectId);

            String reviewContent = codeReviewAgent.review(changes, codeReviewProperties.getMaxDiffChars());
            String noteBody = CodeReviewPromptBuilder.formatReviewNote(reviewContent);
            gitLabApiService.createMergeRequestNote(projectId, mrIid, noteBody);

            log.info("审查完成并已评论 MR !{}", mrIid);

        } catch (Exception e) {
            log.error("MR 审查失败, projectId={}, mrIid={}", projectId, mrIid, e);
        }
    }

    private void logSummary(long mrIid, MergeRequestChangesResponse changes) {
        log.info("MR !{} {} 个文件变更 ({} → {})",
                mrIid, changes.getChanges().size(),
                changes.getSourceBranch(), changes.getTargetBranch());

        if (log.isDebugEnabled()) {
            for (FileChange c : changes.getChanges()) {
                log.debug("  {} → {} | new={} renamed={} deleted={}",
                        c.getOldPath(), c.getNewPath(),
                        c.isNewFile(), c.isRenamedFile(), c.isDeletedFile());
            }
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

}
