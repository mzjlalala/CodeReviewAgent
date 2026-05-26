package cn.com.pcauto.service.serviceImpl;

import cn.com.pcauto.config.GitLabProperties;
import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;
import cn.com.pcauto.service.GitLabApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitLabApiServiceImpl implements GitLabApiService {

    private final GitLabProperties gitLabProperties;
    private final RestTemplate restTemplate;

    @Override
    public MergeRequestChangesResponse getMergeRequestChanges(Long projectId, Long mergeRequestIid) {
        if (!StringUtils.hasText(gitLabProperties.getPrivateToken())) {
            throw new IllegalStateException("未配置 gitlab.private-token，无法调用 GitLab API");
        }

        String baseUrl = trimTrailingSlash(gitLabProperties.getBaseUrl());
        String url = String.format("%s/api/v4/projects/%d/merge_requests/%d/changes",
                baseUrl, projectId, mergeRequestIid);

        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", gitLabProperties.getPrivateToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("请求 GitLab MR changes: projectId={}, mrIid={}, url={}", projectId, mergeRequestIid, url);

        try {
            ResponseEntity<MergeRequestChangesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MergeRequestChangesResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("调用 GitLab API 失败: {}", url, e);
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
