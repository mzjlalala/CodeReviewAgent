package cn.com.pcauto.llm.dto;

import lombok.Data;

@Data
public class ChatRequest {

    private String systemPrompt;

    private String userMessage;

    /**
     * 覆盖 provider 默认 temperature，null 时使用配置值
     */
    private Double temperature;

}
