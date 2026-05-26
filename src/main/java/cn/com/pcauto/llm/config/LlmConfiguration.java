package cn.com.pcauto.llm.config;

import cn.com.pcauto.llm.factory.LlmClientFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Bean
    public RestTemplate llmRestTemplate(LlmProperties llmProperties, RestTemplateBuilder builder) {
        int timeoutSeconds = resolveTimeoutSeconds(llmProperties);
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Bean
    public LlmClientFactory llmClientFactory(LlmProperties llmProperties, RestTemplate llmRestTemplate) {
        return new LlmClientFactory(llmProperties, llmRestTemplate);
    }

    private static int resolveTimeoutSeconds(LlmProperties llmProperties) {
        if (llmProperties.getProviders() == null || llmProperties.getActiveProvider() == null) {
            return 120;
        }
        LlmProperties.ProviderConfig active = llmProperties.getProviders()
                .get(llmProperties.getActiveProvider());
        if (active != null && active.getTimeoutSeconds() != null && active.getTimeoutSeconds() > 0) {
            return active.getTimeoutSeconds();
        }
        return 120;
    }

}
