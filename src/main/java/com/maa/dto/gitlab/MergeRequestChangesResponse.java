package com.maa.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequestChangesResponse {

    private Long id;

    private Long iid;

    @JsonProperty("project_id")
    private Long projectId;

    private String title;

    @JsonProperty("source_branch")
    private String sourceBranch;

    @JsonProperty("target_branch")
    private String targetBranch;

    private List<FileChange> changes;

}
