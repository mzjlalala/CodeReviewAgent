package cn.com.pcauto.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileChange {

    @JsonProperty("old_path")
    private String oldPath;

    @JsonProperty("new_path")
    private String newPath;

    private String diff;

    @JsonProperty("new_file")
    private boolean newFile;

    @JsonProperty("renamed_file")
    private boolean renamedFile;

    @JsonProperty("deleted_file")
    private boolean deletedFile;

}
