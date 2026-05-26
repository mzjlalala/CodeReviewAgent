package cn.com.pcauto.llm.client;

import cn.com.pcauto.llm.dto.ChatRequest;
import cn.com.pcauto.llm.dto.ChatResponse;

public interface LlmClient {

    /**
     * @return 厂商标识，如 deepseek / qwen / glm
     */
    String providerId();

    ChatResponse chat(ChatRequest request);

}
