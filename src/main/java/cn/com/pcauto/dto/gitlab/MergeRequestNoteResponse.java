package cn.com.pcauto.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequestNoteResponse {

    private Long id;

    private String body;

    @JsonProperty("noteable_type")
    private String noteableType;

    @JsonProperty("noteable_iid")
    private Long noteableIid;

}
