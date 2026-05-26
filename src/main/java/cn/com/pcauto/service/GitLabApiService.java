package cn.com.pcauto.service;

import cn.com.pcauto.dto.gitlab.MergeRequestChangesResponse;

public interface GitLabApiService {

    /**
     * 获取合并请求的文件变更及 diff
     *
     * @see <a href="https://docs.gitlab.com/ee/api/merge_requests.html#get-single-merge-request-changes">Get single MR changes</a>
     */
    MergeRequestChangesResponse getMergeRequestChanges(Long projectId, Long mergeRequestIid);

    /**
     * 在 Merge Request 下创建一条评论（Note）
     *
     * @see <a href="https://docs.gitlab.com/ee/api/notes.html#create-new-merge-request-note">Create MR note</a>
     */
    void createMergeRequestNote(Long projectId, Long mergeRequestIid, String body);

}
