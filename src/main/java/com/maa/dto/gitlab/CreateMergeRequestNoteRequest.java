package com.maa.dto.gitlab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMergeRequestNoteRequest {

    private String body;

}
