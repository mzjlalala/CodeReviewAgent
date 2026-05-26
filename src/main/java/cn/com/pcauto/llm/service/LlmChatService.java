package cn.com.pcauto.llm.service;

import cn.com.pcauto.llm.dto.ChatRequest;
import cn.com.pcauto.llm.dto.ChatResponse;
import cn.com.pcauto.llm.factory.LlmClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final LlmClientFactory llmClientFactory;

    public ChatResponse chat(ChatRequest request) {
        return llmClientFactory.getActiveClient().chat(request);
    }

    public String chat(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt(systemPrompt);
        request.setUserMessage(userMessage);
        return chat(request).getContent();
    }

}
