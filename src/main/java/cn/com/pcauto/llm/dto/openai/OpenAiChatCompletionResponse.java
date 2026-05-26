package cn.com.pcauto.llm.dto.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatCompletionResponse {

    private String model;

    private List<Choice> choices;

    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        private Message message;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {

            private String role;

            private String content;

        }

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {

        @JsonProperty("total_tokens")
        private Integer totalTokens;

    }

}
