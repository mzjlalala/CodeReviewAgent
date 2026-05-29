package com.maa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gitlab")
public class GitLabProperties {

    /**
     * GitLab 实例地址，例如 https://gitlab.example.com
     */
    private String baseUrl;

    /**
     * Personal Access Token / Project Access Token，需 api 读权限
     */
    private String privateToken;

    /**
     * Webhook 配置的 Secret Token
     */
    private String webhookSecret;

}
