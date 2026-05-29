package com.maa.service.serviceImpl;

import com.maa.common.dto.gitlab.CreateMergeRequestNoteRequest;
import com.maa.common.dto.gitlab.MergeRequestChangesResponse;
import com.maa.common.dto.gitlab.MergeRequestNoteResponse;
import com.maa.config.GitLabProperties;
import com.maa.service.GitLabApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitLabApiServiceImpl implements GitLabApiService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final GitLabProperties gitLabProperties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public MergeRequestChangesResponse getMergeRequestChanges(Long projectId, Long mergeRequestIid) {
        ensureTokenConfigured();

        String url = buildUrl("/projects/%d/merge_requests/%d/changes", projectId, mergeRequestIid);
        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", gitLabProperties.getPrivateToken())
                .get()
                .build();

        log.info("请求 GitLab MR changes: projectId={}, mrIid={}", projectId, mergeRequestIid);
        return execute(request, MergeRequestChangesResponse.class);
    }

    @Override
    public void createMergeRequestNote(Long projectId, Long mergeRequestIid, String body) {
        ensureTokenConfigured();
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        String url = buildUrl("/projects/%d/merge_requests/%d/notes", projectId, mergeRequestIid);

        CreateMergeRequestNoteRequest noteRequest = new CreateMergeRequestNoteRequest(body.trim());
        RequestBody requestBody = toJsonBody(noteRequest);

        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", gitLabProperties.getPrivateToken())
                .post(requestBody)
                .build();

        log.info("创建 MR 评论: projectId={}, mrIid={}, bodyLength={}", projectId, mergeRequestIid, body.length());

        MergeRequestNoteResponse note = execute(request, MergeRequestNoteResponse.class);
        if (note != null) {
            log.info("MR 评论已创建: noteId={}", note.getId());
        }
    }

    @Override
    public String getRawFileContent(Long projectId, String filePath, String ref) throws UnsupportedEncodingException {
        ensureTokenConfigured();

        String encodedPath = java.net.URLEncoder.encode(filePath, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String url = buildUrl("/projects/%d/repository/files/%s/raw?ref=%s", projectId, encodedPath, ref);

        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", gitLabProperties.getPrivateToken())
                .get()
                .build();

        log.info("请求 GitLab 原始文件: projectId={}, path={}, ref={}", projectId, filePath, ref);
        return execute(request, String.class);
    }

    @Override
    public String getBaseUrl() {
        return gitLabProperties.getBaseUrl();
    }

    // ────────── 内部工具方法 ──────────

    private void ensureTokenConfigured() {
        if (!StringUtils.hasText(gitLabProperties.getPrivateToken())) {
            throw new IllegalStateException("未配置 gitlab.private-token，无法调用 GitLab API");
        }
    }

    private String buildUrl(String pathTemplate, Object... args) {
        String baseUrl = gitLabProperties.getBaseUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String path = String.format(pathTemplate, args);
        return baseUrl + "/api/v4" + path;
    }

    private <T> T execute(Request request, Class<T> responseType) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("GitLab API 返回错误: status={}, url={}, body={}",
                        response.code(), request.url(), errorBody);
                throw new IOException("GitLab API error: " + response.code() + " " + response.message());
            }
            if (response.body() == null) {
                return null;
            }
            String body = response.body().string();
            if (responseType == String.class) {
                return responseType.cast(body);
            }
            return objectMapper.readValue(body, responseType);
        } catch (IOException e) {
            log.error("调用 GitLab API 失败: {}", request.url(), e);
            throw new RuntimeException("GitLab API call failed: " + request.url(), e);
        }
    }

    private RequestBody toJsonBody(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            return RequestBody.create(json, JSON);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }
}
