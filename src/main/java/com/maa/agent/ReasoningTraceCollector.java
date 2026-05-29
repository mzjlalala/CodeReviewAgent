package com.maa.agent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 收集 ReactAgent ReAct 循环中的推理过程。
 */
@Component
public class ReasoningTraceCollector {

    private final ThreadLocal<List<ReasoningTraceCollector.Step>> trace =
            ThreadLocal.withInitial(ArrayList::new);

    public void addThought(String content) {
        trace.get().add(new Step("🤔 思考", content));
    }

    public void addToolCall(String toolName, String args, String result) {
        trace.get().add(new Step("🔧 调用工具: " + toolName,
                "参数: " + args + "\n结果: " + truncate(result, 500)));
    }

    public void addAnswer(String content) {
        trace.get().add(new Step("📝 输出", content));
    }

    public List<Step> getSteps() {
        return Collections.unmodifiableList(trace.get());
    }

    public void clear() {
        trace.remove();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...(截断" + (s.length() - maxLen) + "字符)";
    }

    public record Step(String phase, String content) {}

}
