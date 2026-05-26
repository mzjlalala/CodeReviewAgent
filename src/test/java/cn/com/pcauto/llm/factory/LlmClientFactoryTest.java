package cn.com.pcauto.llm.factory;

import cn.com.pcauto.llm.config.LlmProperties;
import cn.com.pcauto.llm.exception.LlmException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmClientFactoryTest {

    @Test
    void getActiveClient_shouldReturnConfiguredProvider() {
        LlmProperties properties = new LlmProperties();
        properties.setActiveProvider("qwen");

        LlmProperties.ProviderConfig qwen = new LlmProperties.ProviderConfig();
        qwen.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        qwen.setApiKey("qwen-key");
        qwen.setModel("qwen-plus");

        properties.getProviders().put("qwen", qwen);

        LlmClientFactory factory = new LlmClientFactory(properties, new RestTemplate());

        assertEquals("qwen", factory.getActiveClient().providerId());
    }

    @Test
    void getActiveClient_shouldFailWhenProviderMissing() {
        LlmProperties properties = new LlmProperties();
        properties.setActiveProvider("unknown");

        LlmClientFactory factory = new LlmClientFactory(properties, new RestTemplate());

        assertThrows(LlmException.class, factory::getActiveClient);
    }

}
