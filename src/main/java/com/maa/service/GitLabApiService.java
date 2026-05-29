package com.maa.service;

import com.maa.dto.gitlab.MergeRequestChangesResponse;

import java.io.UnsupportedEncodingException;

public interface GitLabApiService {

    /**
     * 获取 GitLab 实例地址
     */
    String getBaseUrl();

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

    /**
     * 获取仓库中指定文件的原始内容
     *
     * @see <a href="https://docs.gitlab.com/ee/api/repository_files.html#get-raw-file-from-repository">Get raw file</a>
     */
    String getRawFileContent(Long projectId, String filePath, String ref) throws UnsupportedEncodingException;

}
