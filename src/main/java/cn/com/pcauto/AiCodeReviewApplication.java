package cn.com.pcauto;

import cn.com.pcauto.config.GitLabProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GitLabProperties.class)
public class AiCodeReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeReviewApplication.class, args);
        System.out.println("启动成功！");
        System.out.println("启动成功！");
        System.out.println("启动成功！");
        System.out.println("启动成功！");
        System.out.println("启动成功！");
    }

}
