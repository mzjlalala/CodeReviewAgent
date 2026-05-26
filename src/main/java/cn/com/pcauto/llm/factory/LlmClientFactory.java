package cn.com.pcauto.llm.factory;

import cn.com.pcauto.llm.client.LlmClient;
import cn.com.pcauto.llm.client.OpenAiCompatibleClient;
import cn.com.pcauto.llm.config.LlmProperties;
import cn.com.pcauto.llm.exception.LlmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class LlmClientFactory {

    private final LlmProperties properties;
    private final Map<String, LlmClient> clients;

    public LlmClientFactory(LlmProperties properties, RestTemplate llmRestTemplate) {
        this.properties = properties;
        this.clients = buildClients(properties, llmRestTemplate);
        log.info("LLM 客户端已初始化: providers={}, active={}",
                clients.keySet(), properties.getActiveProvider());
    }

    public LlmClient getActiveClient() {
        String activeProvider = properties.getActiveProvider();
        if (!StringUtils.hasText(activeProvider)) {
            throw new LlmException("未配置 llm.active-provider");
        }

        LlmClient client = clients.get(activeProvider);
        if (client == null) {
            throw new LlmException("未找到 LLM provider 配置: " + activeProvider
                    + "，可用: " + clients.keySet());
        }
        return client;
    }

    public Map<String, LlmClient> getClients() {
        return Collections.unmodifiableMap(clients);
    }

    private static Map<String, LlmClient> buildClients(LlmProperties properties,
                                                       RestTemplate llmRestTemplate) {
        Map<String, LlmClient> result = new LinkedHashMap<>();
        if (properties.getProviders() == null) {
            return result;
        }

        for (Map.Entry<String, LlmProperties.ProviderConfig> entry : properties.getProviders().entrySet()) {
            String providerId = entry.getKey();
            LlmProperties.ProviderConfig config = entry.getValue();
            if (config == null) {
                continue;
            }
            result.put(providerId, new OpenAiCompatibleClient(providerId, config, llmRestTemplate));
        }
        return result;
    }

}
