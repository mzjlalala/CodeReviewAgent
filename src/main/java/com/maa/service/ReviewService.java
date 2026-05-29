package com.maa.service;

import com.maa.agent.CodeReviewAgent;
import com.maa.config.CodeReviewProperties;
import com.maa.dto.gitlab.FileChange;
import com.maa.dto.gitlab.MergeRequestChangesResponse;
import com.maa.review.CodeReviewPromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 审核服务 —— 解析 Webhook payload，编排审查流程。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final Set<String> DIFF_ACTIONS = new HashSet<>(Arrays.asList(
            "open", "update", "reopen"
    ));

    private final GitLabApiService gitLabApiService;
    private final CodeReviewAgent codeReviewAgent;
    private final CodeReviewProperties codeReviewProperties;

    /**
     * 处理 MR Webhook 事件，判断是否需要拉取 diff 并触发审查。
     */
    public void handleMergeRequestEvent(JsonNode payload) {
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

            // 输出 Agent 推理过程到日志
            var trace = codeReviewAgent.getLastTrace();
            if (trace != null && !trace.isEmpty()) {
                log.info("Agent 推理过程 (MR !{}) 共 {} 步:", mrIid, trace.size());
                for (var step : trace) {
                    log.info("  {} {}", step.phase(), step.content());
                }
            }

            String noteBody = CodeReviewPromptBuilder.formatReviewNote(reviewContent);
            gitLabApiService.createMergeRequestNote(projectId, mrIid, noteBody);

            log.info("审查完成并已评论 MR !{}", mrIid);

        } catch (Exception e) {
            log.error("MR 审查失败, projectId={}, mrIid={}", projectId, mrIid, e);
        }
    }

    /**
     * 记录合并请求的文件变更摘要信息。
     * <p>
     * 在 INFO 级别输出 MR ID、变更文件数量以及源分支到目标分支的变更信息；
     * 在 DEBUG 级别详细输出每个文件的路径变更及类型标识（新建/重命名/删除）。
     *
     * @param mrIid 合并请求的内部 ID
     * @param changes 合并请求的文件变更响应对象，包含变更列表和分支信息
     */
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
