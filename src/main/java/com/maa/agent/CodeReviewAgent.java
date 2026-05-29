package com.maa.agent;

import com.maa.agent.tools.ReviewAgentTools;
import com.maa.dto.gitlab.MergeRequestChangesResponse;
import com.maa.review.CodeReviewPromptBuilder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 代码审查 Agent — 基于 Spring AI Alibaba ReactAgent。
 * ReAct 模式：思考 → 工具调用 → 再思考 → 输出结构化审查报告。
 */
@Service
@Slf4j
public class CodeReviewAgent {

    private final ReactAgent agent;
    private final ReasoningTraceCollector traceCollector;

    private static final String SYSTEM_PROMPT = """
            你是一名资深代码审查专家 Agent。
            
            ## 你的能力
            - 分析 Merge Request 的代码变更（diff）
            - 可通过内置工具获取变更文件的完整源代码，以便判断变更对原有代码的影响
            - 输出结构化审查报告

            ## 审查流程
            1. 阅读 MR 信息中提供的 GitLab 项目 ID、源分支名、各文件 diff
            2. 对需要更多上下文才能判断的文件，调用 fetchFullFileContent（参数值采用 MR 信息中的内容）
            3. 综合所有信息，输出结构化审查报告
            
            ## 输出格式（用中文）
            ### 总体评价
            （简要总结本次变更的质量与风险）
            
            ### 潜在问题
            （按严重程度排序：[高/中/低]，说明位置、原因与建议）
            
            ### 改进建议
            （可操作的建议，非必须可不写）
            
            ### 值得肯定的写法
            （如有）
            
            若 diff 被截断，请在开头说明。未发现问题时也要明确说明"未发现明显问题"。
            """;

    public CodeReviewAgent(ChatModel chatModel,
                           ReviewAgentTools reviewAgentTools,
                           ReviewReasoningInterceptor reasoningInterceptor,
                           ReasoningTraceCollector traceCollector) {
        this.traceCollector = traceCollector;
        this.agent = ReactAgent.builder()
                .name("code-review")
                .model(chatModel)
                .systemPrompt(SYSTEM_PROMPT)
                .methodTools(reviewAgentTools)
                .interceptors(
                        reasoningInterceptor.modelInterceptor(),
                        reasoningInterceptor.toolInterceptor()
                )
                .build();
        log.info("ReactAgent 已初始化: code-review（含推理追踪）");
    }

    /**
     * 对 MR 变更执行 ReAct Agent 式代码审查。
     */
    public String review(MergeRequestChangesResponse changes, int maxDiffChars) {
        String userMessage = CodeReviewPromptBuilder.buildUserMessage(changes, maxDiffChars);

        log.info("ReactAgent 开始审查 MR !{}, 文件数={}, prompt长度={}",
                changes.getIid(), changes.getChanges().size(), userMessage.length());

        try {
            AssistantMessage result = agent.call(userMessage);
            String content = result.getText();

            log.info("ReactAgent 审查完成 MR !{}, 报告长度={}", changes.getIid(),
                    content != null ? content.length() : 0);
            return content;
        } catch (Exception e) {
            log.error("ReactAgent 审查异常 MR !{}", changes.getIid(), e);
            return "Agent 审查失败: " + e.getMessage();
        }
    }

    /**
     * 获取最近一次审查的推理过程。
     */
    public List<ReasoningTraceCollector.Step> getLastTrace() {
        return traceCollector.getSteps();
    }

}
