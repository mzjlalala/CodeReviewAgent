package cn.com.pcauto.service.serviceImpl;

import cn.com.pcauto.config.CodeReviewProperties;
import cn.com.pcauto.dto.gitlab.FileChange;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
import cn.com.pcauto.llm.exception.LlmException;
import cn.com.pcauto.llm.service.LlmChatService;
import cn.com.pcauto.review.CodeReviewPromptBuilder;
import cn.com.pcauto.service.GitLabApiService;
import cn.com.pcauto.service.MergeRequestReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeRequestReviewServiceImpl implements MergeRequestReviewService {

    private static final Set<String> DIFF_ACTIONS = new HashSet<>(Arrays.asList(
            "open", "update", "reopen"
    ));

    private final GitLabApiService gitLabApiService;
    private final LlmChatService llmChatService;
    private final CodeReviewProperties codeReviewProperties;

    @Override
    public void handleMergeRequestWebhook(JsonNode payload) {
        JsonNode objectAttributes = payload.get("object_attributes");
        if (objectAttributes == null || objectAttributes.isNull()) {
            log.warn("MR Webhook 缺少 object_attributes，跳过");
            return;
        }

        String action = textOrEmpty(objectAttributes, "action");
        String title = textOrEmpty(objectAttributes, "title");
        long mrIid = objectAttributes.get("iid").asLong();

        JsonNode projectNode = payload.get("project");
        if (projectNode == null || projectNode.isNull()) {
            log.warn("MR Webhook 缺少 project，跳过");
            return;
        }
        long projectId = projectNode.get("id").asLong();

        log.info("收到 MR Webhook: action={}, title={}, projectId={}, mrIid={}",
                action, title, projectId, mrIid);

        if (!DIFF_ACTIONS.contains(action)) {
            log.info("MR 动作 {} 无需拉取 diff，跳过", action);
            return;
        }

        reviewMergeRequest(projectId, mrIid);
    }

    private void reviewMergeRequest(long projectId, long mrIid) {
        try {
            MergeRequestChangesResponse changes =
                    gitLabApiService.getMergeRequestChanges(projectId, mrIid);

            if (changes == null || changes.getChanges() == null || changes.getChanges().isEmpty()) {
                log.info("MR !{} 无文件变更", mrIid);
                return;
            }

            logChangesSummary(mrIid, changes);

            if (!codeReviewProperties.isEnabled()) {
                log.info("AI 代码审查已关闭，跳过 MR !{}", mrIid);
                return;
            }

            runAiReview(projectId, mrIid, changes);
        } catch (Exception e) {
            log.error("MR 审查失败, projectId={}, mrIid={}", projectId, mrIid, e);
        }
    }

    private void logChangesSummary(long mrIid, MergeRequestChangesResponse changes) {
        log.info("Merge Request 请求id：{} 共 {} 个文件变更 ({} -> {})",
                mrIid,
                changes.getChanges().size(),
                changes.getSourceBranch(),
                changes.getTargetBranch());

        for (FileChange change : changes.getChanges()) {
            int diffLen = change.getDiff() == null ? 0 : change.getDiff().length();
            log.info("  - {} -> {} | new={} renamed={} deleted={} | diffChars={}",
                    change.getOldPath(),
                    change.getNewPath(),
                    change.isNewFile(),
                    change.isRenamedFile(),
                    change.isDeletedFile(),
                    diffLen);
            if (log.isDebugEnabled() && change.getDiff() != null) {
                log.debug("diff content:\n{}", change.getDiff());
            }
        }
    }

    private void runAiReview(long projectId, long mrIid, MergeRequestChangesResponse changes) {
        String userMessage = CodeReviewPromptBuilder.buildUserMessage(changes, codeReviewProperties.getMaxDiffChars());

        log.info("开始 AI 审查 MR !{}, promptChars={}", mrIid, userMessage.length());

        try {
            String reviewContent = llmChatService.chat(CodeReviewPromptBuilder.systemPrompt(), userMessage);
            String noteBody = CodeReviewPromptBuilder.formatReviewNote(reviewContent);

            gitLabApiService.createMergeRequestNote(projectId, mrIid, noteBody);
            log.info("AI 审查完成并已评论到 MR !{}", mrIid);
        } catch (LlmException e) {
            log.error("AI 审查调用失败, MR !{}: {}", mrIid, e.getMessage(), e);
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

}
