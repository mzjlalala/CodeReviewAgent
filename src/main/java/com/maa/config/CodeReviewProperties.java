package com.maa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "code-review")
public class CodeReviewProperties {

    /**
     * 是否启用 AI 代码审查
     */
    private boolean enabled = true;

    /**
     * 发送给 LLM 的 diff 最大字符数，超出则截断
     */
    private int maxDiffChars = 80000;

}
