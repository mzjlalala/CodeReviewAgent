package cn.com.pcauto.review;

import cn.com.pcauto.dto.gitlab.FileChange;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeReviewPromptBuilderTest {

    @Test
    void buildUserMessage_shouldIncludeMrMetaAndDiff() {
        FileChange change = new FileChange();
        change.setOldPath("src/Foo.java");
        change.setNewPath("src/Foo.java");
        change.setDiff("@@ -1 +1 @@\n-old\n+new");

        MergeRequestChangesResponse changes = new MergeRequestChangesResponse();
        changes.setTitle("fix foo");
        changes.setProjectId(99L);
        changes.setSourceBranch("feature/foo");
        changes.setTargetBranch("main");
        changes.setChanges(Collections.singletonList(change));

        String message = CodeReviewPromptBuilder.buildUserMessage(changes, 80000);

        assertTrue(message.contains("MR 标题: fix foo"));
        assertTrue(message.contains("GitLab 项目 ID: 99"));
        assertTrue(message.contains("源分支: feature/foo → 目标分支: main"));
        assertTrue(message.contains("### 文件: src/Foo.java"));
        assertTrue(message.contains("```diff"));
        assertTrue(message.contains("+new"));
    }

    @Test
    void buildUserMessage_shouldTruncateWhenExceedsLimit() {
        FileChange change = new FileChange();
        change.setNewPath("Large.java");
        change.setDiff(String.join("", Collections.nCopies(500, "x")));

        MergeRequestChangesResponse changes = new MergeRequestChangesResponse();
        changes.setChanges(Collections.singletonList(change));

        String message = CodeReviewPromptBuilder.buildUserMessage(changes, 100);

        assertTrue(message.contains("diff 因长度限制未完整发送"));
    }

    @Test
    void formatReviewNote_shouldWrapContent() {
        String note = CodeReviewPromptBuilder.formatReviewNote("审查通过");

        assertTrue(note.startsWith("## AI 代码审查"));
        assertTrue(note.contains("审查通过"));
        assertTrue(note.contains("AiCodeReview 自动生成"));
    }

}
