package com.maa.agent.tools;

import com.maa.service.GitLabApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 代码审查 Agent 的工具集。
 * Spring AI 自动将这些 @Tool 方法注册为 OpenAI Function Calling 工具，
 * 让 Agent 能按需调用。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewAgentTools {

    private final GitLabApiService gitLabApiService;

    /**
     * 从 GitLab 仓库获取指定文件的完整内容（用于获取 diff 之外的上下文）。
     */
    @Tool(description = "从 GitLab 仓库获取指定文件的完整源代码内容，用于查看变更行周围的上下文")
    public String fetchFullFileContent(
            @ToolParam(description = "GitLab 项目 ID") Long projectId,
            @ToolParam(description = "文件路径，如 src/main/java/App.java") String filePath,
            @ToolParam(description = "分支名或 commit SHA，如 master 或 feature/my-change") String ref) {
        log.info("Agent 请求文件内容: projectId={}, path={}, ref={}", projectId, filePath, ref);
        try {
            return gitLabApiService.getRawFileContent(projectId, filePath, ref);
        } catch (Exception e) {
            log.error("获取文件内容失败", e);
            return "错误：无法获取文件内容 - " + e.getMessage();
        }
    }

}
