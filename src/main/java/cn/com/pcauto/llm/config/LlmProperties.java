package cn.com.pcauto.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /**
     * 当前启用的厂商标识，对应 providers 下的 key，如 deepseek / qwen / glm
     */
    private String activeProvider;

    /**
     * 各厂商配置，key 为 providerId
     */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    @Data
    public static class ProviderConfig {

        /**
         * API 根地址，不含 /chat/completions，例如 https://api.deepseek.com/v1
         */
        private String baseUrl;

        private String apiKey;

        private String model;

        private Double temperature = 0.7;

        private Integer timeoutSeconds = 120;

    }

}
