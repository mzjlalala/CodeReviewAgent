package cn.com.pcauto.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {

    private String content;

    private String providerId;

    private String model;

    private Integer totalTokens;

}
