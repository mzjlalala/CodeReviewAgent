package cn.com.pcauto.service;

import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;

public interface GitLabApiService {

    /**
     * 获取合并请求的文件变更及 diff
     *
     * @see <a href="https://docs.gitlab.com/ee/api/merge_requests.html#get-single-merge-request-changes">Get single MR changes</a>
     */
    MergeRequestChangesResponse getMergeRequestChanges(Long projectId, Long mergeRequestIid);

}
