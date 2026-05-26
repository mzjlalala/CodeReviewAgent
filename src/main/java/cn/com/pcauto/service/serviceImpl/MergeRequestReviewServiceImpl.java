package cn.com.pcauto.service.serviceImpl;

import cn.com.pcauto.dto.gitlab.FileChange;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
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

        fetchAndLogDiff(projectId, mrIid);
    }

    private void fetchAndLogDiff(long projectId, long mrIid) {
        try {
            MergeRequestChangesResponse changes =
                    gitLabApiService.getMergeRequestChanges(projectId, mrIid);

            if (changes == null || changes.getChanges() == null || changes.getChanges().isEmpty()) {
                log.info("MR !{} 无文件变更", mrIid);
                return;
            }

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

            // TODO: 将 changes 交给 AI 审查模块
        } catch (Exception e) {
            log.error("获取 MR diff 失败, projectId={}, mrIid={}", projectId, mrIid, e);
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

}
