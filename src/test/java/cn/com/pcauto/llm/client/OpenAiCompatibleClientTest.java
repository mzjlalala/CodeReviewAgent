package cn.com.pcauto.llm.client;

import cn.com.pcauto.llm.config.LlmProperties;
import cn.com.pcauto.llm.dto.ChatRequest;
import cn.com.pcauto.llm.dto.ChatResponse;
import cn.com.pcauto.llm.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void chat_shouldReturnAssistantContent() {
        LlmProperties.ProviderConfig config = providerConfig();
        OpenAiCompatibleClient client = new OpenAiCompatibleClient("deepseek", config, restTemplate);

        mockServer.expect(requestTo("https://api.deepseek.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess(
                        "{"
                                + "\"model\":\"deepseek-chat\","
                                + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"审查通过\"}}],"
                                + "\"usage\":{\"total_tokens\":42}"
                                + "}",
                        MediaType.APPLICATION_JSON));

        ChatRequest request = new ChatRequest();
        request.setSystemPrompt("你是代码审查助手");
        request.setUserMessage("请审查这段 diff");

        ChatResponse response = client.chat(request);

        assertEquals("审查通过", response.getContent());
        assertEquals("deepseek", response.getProviderId());
        assertEquals("deepseek-chat", response.getModel());
        assertEquals(42, response.getTotalTokens());
        mockServer.verify();
    }

    @Test
    void chat_shouldRejectEmptyUserMessage() {
        LlmProperties.ProviderConfig config = providerConfig();
        OpenAiCompatibleClient client = new OpenAiCompatibleClient("deepseek", config, restTemplate);

        ChatRequest request = new ChatRequest();
        request.setUserMessage("  ");

        assertThrows(LlmException.class, () -> client.chat(request));
    }

    private static LlmProperties.ProviderConfig providerConfig() {
        LlmProperties.ProviderConfig config = new LlmProperties.ProviderConfig();
        config.setBaseUrl("https://api.deepseek.com/v1");
        config.setApiKey("test-api-key");
        config.setModel("deepseek-chat");
        config.setTemperature(0.3);
        return config;
    }

}
