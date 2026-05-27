package cn.com.pcauto.agent;

import cn.com.pcauto.agent.tools.ReviewAgentTools;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
import cn.com.pcauto.review.CodeReviewPromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 代码审查 Agent。
 * 使用 Spring AI ChatClient + Tool Calling 实现 Agent 式审查：
 * <ol>
 *   <li>Agent 收到 MR 变更信息 + 各文件 diff</li>
 *   <li>可自主调用 {@code fetchFullFileContent} 工具获取文件完整上下文</li>
 *   <li>综合所有信息输出结构化审查报告</li>
 * </ol>
 */
@Service
@Slf4j
public class CodeReviewAgent {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是一名资深代码审查专家 Agent。
            
            ## 你的能力
            - 分析 Merge Request 的代码变更（diff）
            - 可通过调用工具 fetchFullFileContent 获取变更文件的完整源代码（超越 diff 范围），
              以便判断变更对原有代码上下文的影响
            - 输出结构化审查报告
            
            ## 审查流程
            1. 阅读下方提供的 MR 信息与各文件 diff
            2. 对于你认为需要更多上下文才能判断的文件，调用 fetchFullFileContent 工具
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

    public CodeReviewAgent(ChatClient.Builder chatClientBuilder,
                            ReviewAgentTools reviewAgentTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(reviewAgentTools)
                .build();
    }

    /**
     * 对 MR 变更执行 Agent 式代码审查。
     *
     * @param changes     MR 变更信息（包含文件列表与 diff）
     * @param maxDiffChars diff 发送给 LLM 的最大字符数
     * @return 审查报告 Markdown 文本
     */
    public String review(MergeRequestChangesResponse changes, int maxDiffChars) {
        String userMessage = CodeReviewPromptBuilder.buildUserMessage(changes, maxDiffChars);

        log.info("Agent 开始审查 MR !{}, 文件数={}, prompt长度={}",
                changes.getIid(),
                changes.getChanges().size(),
                userMessage.length());

        String result = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        log.info("Agent 审查完成 MR !{}, 报告长度={}", changes.getIid(),
                result != null ? result.length() : 0);
        return result;
    }

}
