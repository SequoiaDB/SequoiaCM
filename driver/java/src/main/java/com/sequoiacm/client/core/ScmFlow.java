package com.sequoiacm.client.core;

import org.bson.BSONObject;

/**
 * Scm flow.
 */
public class ScmFlow {

    private static final String WORK_SPACE = "workspace_name";
    private static final String UPLOAD_FLOW = "upload_flow";
    private static final String DOWNLOAD_FLOW = "download_flow";

    private String workspaceName;

    private long uploadFlow;

    private long downloadFlow;

    /**
     * Create a instance of ScmFlow.
     *
     * @param obj
     *            a bson containing basic information about scm flow.
     */
    public ScmFlow(BSONObject obj) {

        Object temp = null;

        temp = obj.get(WORK_SPACE);
        if (null != temp) {
            setWorkspaceName(temp.toString());
        }

        temp = obj.get(UPLOAD_FLOW);
        if (null != temp) {
            setUploadFlow(Long.parseLong(temp.toString()));
        }

        temp = obj.get(DOWNLOAD_FLOW);
        if (null != temp) {
            setDownloadFlow(Long.parseLong(temp.toString()));
        }
    }

    /**
     * Gets workspace name.
     *
     * @return workspace name.
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Sets the workspace name.
     *
     * @param workspaceName
     *            workspace name.
     */
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    /**
     * Return the upload flow.
     *
     * @return upload flow.
     */
    public long getUploadFlow() {
        return uploadFlow;
    }

    /**
     * Sets the upload flow.
     *
     * @param uploadFlow upload flow.
     */
    public void setUploadFlow(long uploadFlow) {
        this.uploadFlow = uploadFlow;
    }

    /**
     * Return the download flow.
     *
     * @return download flow.
     */
    public long getDownloadFlow() {
        return downloadFlow;
    }

    /**
     * Sets the download flow.
     *
     * @param downloadFlow
     *            download flow.
     */
    public void setDownloadFlow(long downloadFlow) {
        this.downloadFlow = downloadFlow;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(WORK_SPACE).append(": ").append(workspaceName).append(",");
        sb.append(UPLOAD_FLOW).append(": ").append(uploadFlow).append(",");
        sb.append(DOWNLOAD_FLOW).append(": ").append(downloadFlow);
        sb.append("}");
        return sb.toString();
    }
}
