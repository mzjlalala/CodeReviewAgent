package com.maa;

import com.maa.config.CodeReviewProperties;
import com.maa.config.GitLabProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GitLabProperties.class, CodeReviewProperties.class})
public class AiCodeReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeReviewApplication.class, args);
    }

}
