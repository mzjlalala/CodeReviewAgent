package com.maa.service;

import com.maa.agent.CodeReviewAgent;
import com.maa.config.CodeReviewProperties;
import com.maa.dto.gitlab.FileChange;
import com.maa.dto.gitlab.MergeRequestChangesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeReviewOrchestratorTest {

    @Mock
    private GitLabApiService gitLabApiService;

    @Mock
    private CodeReviewAgent codeReviewAgent;

    @Mock
    private CodeReviewProperties codeReviewProperties;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CodeReviewOrchestrator orchestrator;

    @Test
    void shouldReviewAndPostNoteWhenMrOpened() throws Exception {
        when(codeReviewProperties.isEnabled()).thenReturn(true);
        when(codeReviewProperties.getMaxDiffChars()).thenReturn(80000);

        String payload = "{"
                + "\"object_attributes\":{\"action\":\"open\",\"title\":\"test mr\",\"iid\":12},"
                + "\"project\":{\"id\":99}"
                + "}";

        FileChange change = new FileChange();
        change.setNewPath("App.java");
        change.setDiff("+code");

        MergeRequestChangesResponse changes = new MergeRequestChangesResponse();
        changes.setTitle("test mr");
        changes.setSourceBranch("dev");
        changes.setTargetBranch("main");
        changes.setChanges(Collections.singletonList(change));

        when(gitLabApiService.getMergeRequestChanges(99L, 12L)).thenReturn(changes);
        when(codeReviewAgent.review(eq(changes), anyInt())).thenReturn("未发现明显问题");

        orchestrator.handleWebhook("Merge Request Hook", payload);

        ArgumentCaptor<String> noteCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitLabApiService).createMergeRequestNote(eq(99L), eq(12L), noteCaptor.capture());
        assertTrue(noteCaptor.getValue().contains("未发现明显问题"));
        assertTrue(noteCaptor.getValue().contains("AI 代码审查"));
    }

    @Test
    void shouldSkipReviewWhenDisabled() throws Exception {
        when(codeReviewProperties.isEnabled()).thenReturn(false);

        String payload = "{"
                + "\"object_attributes\":{\"action\":\"open\",\"title\":\"test\",\"iid\":1},"
                + "\"project\":{\"id\":1}"
                + "}";

        FileChange change = new FileChange();
        change.setNewPath("A.java");
        change.setDiff("+a");

        MergeRequestChangesResponse changes = new MergeRequestChangesResponse();
        changes.setChanges(Collections.singletonList(change));

        when(gitLabApiService.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(changes);

        orchestrator.handleWebhook("Merge Request Hook", payload);

        verify(codeReviewAgent, never()).review(eq(changes), anyInt());
        verify(gitLabApiService, never()).createMergeRequestNote(anyLong(), anyLong(), anyString());
    }

}
