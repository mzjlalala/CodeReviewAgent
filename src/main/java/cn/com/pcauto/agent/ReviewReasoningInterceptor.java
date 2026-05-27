package cn.com.pcauto.agent;

import com.alibaba.cloud.ai.graph.agent.interceptor.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

/**
 * ReactAgent 推理过程拦截器 —— 在每个模型调用和工具调用前后记录推理步骤。
 */
@Component
@RequiredArgsConstructor
public class ReviewReasoningInterceptor {

    private final ReasoningTraceCollector traceCollector;

    public ModelInterceptor modelInterceptor() {
        return new ModelInterceptor() {
            @Override
            public ModelResponse interceptModel(ModelRequest req, ModelCallHandler next) {
                ModelResponse resp = next.call(req);

                Object rawMsg = resp.getMessage();
                if (rawMsg instanceof AssistantMessage msg) {
                    if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                        String names = msg.getToolCalls().stream()
                                .map(tc -> tc.name())
                                .reduce((a, b) -> a + ", " + b).orElse("");
                        traceCollector.addThought("决定调用工具: " + names);
                    } else if (msg.getText() != null) {
                        traceCollector.addAnswer(msg.getText());
                    }
                }
                return resp;
            }
            @Override
            public String getName() { return "review-reasoning-model"; }
        };
    }

    public ToolInterceptor toolInterceptor() {
        return new ToolInterceptor() {
            @Override
            public ToolCallResponse interceptToolCall(ToolCallRequest req, ToolCallHandler next) {
                ToolCallResponse resp = next.call(req);
                traceCollector.addToolCall(req.getToolName(), req.getArguments(), resp.getResult());
                return resp;
            }
            @Override
            public String getName() { return "review-reasoning-tool"; }
        };
    }

}
