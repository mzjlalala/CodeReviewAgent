package cn.com.pcauto.llm.client;

import cn.com.pcauto.llm.config.LlmProperties;
import cn.com.pcauto.llm.dto.ChatRequest;
import cn.com.pcauto.llm.dto.ChatResponse;
import cn.com.pcauto.llm.dto.openai.OpenAiChatCompletionRequest;
import cn.com.pcauto.llm.dto.openai.OpenAiChatCompletionResponse;
import cn.com.pcauto.llm.exception.LlmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenAiCompatibleClient implements LlmClient {

    private final String providerId;
    private final LlmProperties.ProviderConfig config;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleClient(String providerId,
                                  LlmProperties.ProviderConfig config,
                                  RestTemplate restTemplate) {
        this.providerId = providerId;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        validateConfig();
        validateRequest(request);

        String url = buildChatCompletionsUrl();
        OpenAiChatCompletionRequest body = buildRequestBody(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<OpenAiChatCompletionRequest> entity = new HttpEntity<>(body, headers);

        log.info("调用 LLM [{}]: model={}, url={}", providerId, config.getModel(), url);

        try {
            ResponseEntity<OpenAiChatCompletionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, OpenAiChatCompletionResponse.class);
            return parseResponse(response.getBody());
        } catch (RestClientException e) {
            log.error("调用 LLM [{}] 失败: {}", providerId, url, e);
            throw new LlmException("调用 LLM [" + providerId + "] 失败: " + e.getMessage(), e);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw new LlmException("LLM provider [" + providerId + "] 未配置 base-url");
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new LlmException("LLM provider [" + providerId + "] 未配置 api-key");
        }
        if (!StringUtils.hasText(config.getModel())) {
            throw new LlmException("LLM provider [" + providerId + "] 未配置 model");
        }
    }

    private void validateRequest(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new LlmException("userMessage 不能为空");
        }
    }

    private String buildChatCompletionsUrl() {
        String baseUrl = config.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private OpenAiChatCompletionRequest buildRequestBody(ChatRequest request) {
        List<OpenAiChatCompletionRequest.Message> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(new OpenAiChatCompletionRequest.Message("system", request.getSystemPrompt()));
        }
        messages.add(new OpenAiChatCompletionRequest.Message("user", request.getUserMessage()));

        Double temperature = request.getTemperature() != null
                ? request.getTemperature()
                : config.getTemperature();

        return OpenAiChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .temperature(temperature)
                .build();
    }

    private ChatResponse parseResponse(OpenAiChatCompletionResponse response) {
        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new LlmException("LLM [" + providerId + "] 返回空响应");
        }

        String content = response.getChoices().get(0).getMessage().getContent();
        if (!StringUtils.hasText(content)) {
            throw new LlmException("LLM [" + providerId + "] 返回内容为空");
        }

        Integer totalTokens = response.getUsage() != null ? response.getUsage().getTotalTokens() : null;

        return ChatResponse.builder()
                .content(content.trim())
                .providerId(providerId)
                .model(response.getModel() != null ? response.getModel() : config.getModel())
                .totalTokens(totalTokens)
                .build();
    }

}
