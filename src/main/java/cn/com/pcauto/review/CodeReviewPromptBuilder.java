package cn.com.pcauto.review;

import cn.com.pcauto.dto.gitlab.FileChange;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 将 MR diff 组装为 LLM / Agent 可理解的 Prompt
 */
public final class CodeReviewPromptBuilder {

    private CodeReviewPromptBuilder() {
    }

    public static String buildUserMessage(MergeRequestChangesResponse changes, int maxDiffChars) {
        StringBuilder sb = new StringBuilder();
        sb.append("请审查以下 Merge Request 代码变更。\n\n");
        appendMrMeta(sb, changes);

        List<FileChange> fileChanges = changes.getChanges();
        int remaining = maxDiffChars;
        boolean truncated = false;

        for (FileChange change : fileChanges) {
            String section = buildFileSection(change);
            if (!StringUtils.hasText(section)) {
                continue;
            }
            if (section.length() > remaining) {
                if (remaining > 200) {
                    sb.append(section, 0, remaining);
                    sb.append("\n... [该文件 diff 已截断]\n");
                }
                truncated = true;
                break;
            }
            sb.append(section);
            remaining -= section.length();
        }

        if (truncated) {
            sb.append("\n\n> 注意：部分 diff 因长度限制未完整发送，请结合未展示内容谨慎结论。\n");
        }
        return sb.toString();
    }

    public static String formatReviewNote(String reviewContent) {
        return "## AI 代码审查\n\n"
                + reviewContent.trim()
                + "\n\n---\n*由 AiCodeReview 自动生成*";
    }

    private static void appendMrMeta(StringBuilder sb, MergeRequestChangesResponse changes) {
        if (StringUtils.hasText(changes.getTitle())) {
            sb.append("MR 标题: ").append(changes.getTitle()).append('\n');
        }
        sb.append("分支: ")
                .append(nullToDash(changes.getSourceBranch()))
                .append(" -> ")
                .append(nullToDash(changes.getTargetBranch()))
                .append('\n');
        sb.append("变更文件数: ").append(changes.getChanges().size()).append("\n\n");
    }

    private static String buildFileSection(FileChange change) {
        String path = StringUtils.hasText(change.getNewPath()) ? change.getNewPath() : change.getOldPath();
        if (!StringUtils.hasText(path)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 文件: ").append(path);
        if (change.isNewFile()) {
            sb.append(" (新文件)");
        } else if (change.isDeletedFile()) {
            sb.append(" (已删除)");
        } else if (change.isRenamedFile()) {
            sb.append(" (重命名自 ").append(change.getOldPath()).append(')');
        }
        sb.append('\n');

        if (StringUtils.hasText(change.getDiff())) {
            sb.append("```diff\n").append(change.getDiff()).append("\n```\n\n");
        } else {
            sb.append("*(无 diff 内容)*\n\n");
        }
        return sb.toString();
    }

    private static String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

}
