package com.maa.service.serviceImpl;

import com.maa.config.GitLabProperties;
import com.maa.dto.gitlab.CreateMergeRequestNoteRequest;
import com.maa.dto.gitlab.MergeRequestChangesResponse;
import com.maa.dto.gitlab.MergeRequestNoteResponse;
import com.maa.service.GitLabApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitLabApiServiceImpl implements GitLabApiService {

    private final GitLabProperties gitLabProperties;
    private final RestTemplate restTemplate;

    @Override
    public MergeRequestChangesResponse getMergeRequestChanges(Long projectId, Long mergeRequestIid) {
        ensureTokenConfigured();

        String baseUrl = trimTrailingSlash(gitLabProperties.getBaseUrl());
        String url = String.format("%s/api/v4/projects/%d/merge_requests/%d/changes",
                baseUrl, projectId, mergeRequestIid);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        log.info("请求 GitLab MR changes: projectId={}, mrIid={}, url={}", projectId, mergeRequestIid, url);

        try {
            ResponseEntity<MergeRequestChangesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MergeRequestChangesResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("调用 GitLab API - GitLab MR changes 失败: {}", url, e);
            throw e;
        }
    }

    @Override
    public void createMergeRequestNote(Long projectId, Long mergeRequestIid, String body) {
        ensureTokenConfigured();
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        String baseUrl = trimTrailingSlash(gitLabProperties.getBaseUrl());
        String url = String.format("%s/api/v4/projects/%d/merge_requests/%d/notes", baseUrl, projectId, mergeRequestIid);

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateMergeRequestNoteRequest requestBody = new CreateMergeRequestNoteRequest(body.trim());
        HttpEntity<CreateMergeRequestNoteRequest> entity = new HttpEntity<>(requestBody, headers);

        log.info("创建 MR 评论: projectId={}, mrIid={}, bodyLength={}", projectId, mergeRequestIid, body.length());

        try {
            ResponseEntity<MergeRequestNoteResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, MergeRequestNoteResponse.class);
            MergeRequestNoteResponse note = response.getBody();
            if (note != null) {
                log.info("MR 评论已创建: noteId={}", note.getId());
            }
        } catch (RestClientException e) {
            log.error("创建 MR 评论失败: {}", url, e);
            throw e;
        }
    }

    private void ensureTokenConfigured() {
        if (!StringUtils.hasText(gitLabProperties.getPrivateToken())) {
            throw new IllegalStateException("未配置 gitlab.private-token，无法调用 GitLab API");
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", gitLabProperties.getPrivateToken());
        return headers;
    }

    @Override
    public String getBaseUrl() {
        return gitLabProperties.getBaseUrl();
    }

    @Override
    public String getRawFileContent(Long projectId, String filePath, String ref) throws UnsupportedEncodingException {
        ensureTokenConfigured();

        String baseUrl = trimTrailingSlash(gitLabProperties.getBaseUrl());
        String encodedPath = java.net.URLEncoder.encode(filePath, String.valueOf(java.nio.charset.StandardCharsets.UTF_8))
                .replace("+", "%20");
        String url = String.format("%s/api/v4/projects/%d/repository/files/%s/raw?ref=%s",
                baseUrl, projectId, encodedPath, ref);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        log.info("请求 GitLab 原始文件: projectId={}, path={}, ref={}", projectId, filePath, ref);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("获取 GitLab 原始文件失败: {}", url, e);
            throw e;
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}
