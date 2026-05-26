package cn.com.pcauto.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface MergeRequestReviewService {

    /**
     * 处理 Merge Request Webhook，并在需要时拉取 diff
     */
    void handleMergeRequestWebhook(JsonNode payload);

}
