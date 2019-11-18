package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.checksum.ChecksumType;

public class BreakpointFile {
    private String workspaceName;
    private String fileName;
    private int siteId;
    private String siteName;
    private ChecksumType checksumType = ChecksumType.NONE;
    private long checksum;
    private String dataId;
    private boolean completed;
    private long uploadSize;
    private String createUser;
    private long createTime;
    private String uploadUser;
    private long uploadTime;

    public String getWorkspaceName() {
        return workspaceName;
    }

    public BreakpointFile setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public BreakpointFile setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public int getSiteId() {
        return siteId;
    }

    public BreakpointFile setSiteId(int siteId) {
        this.siteId = siteId;
        return this;
    }

    public String getSiteName() {
        return siteName;
    }

    public BreakpointFile setSiteName(String siteName) {
        this.siteName = siteName;
        return this;
    }

    public ChecksumType getChecksumType() {
        return checksumType;
    }

    public BreakpointFile setChecksumType(ChecksumType checksumType) {
        this.checksumType = checksumType;
        return this;
    }

    public long getChecksum() {
        return checksum;
    }

    public BreakpointFile setChecksum(long checksum) {
        this.checksum = checksum;
        return this;
    }

    public String getDataId() {
        return dataId;
    }

    public BreakpointFile setDataId(String dataId) {
        this.dataId = dataId;
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public BreakpointFile setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public long getUploadSize() {
        return uploadSize;
    }

    public BreakpointFile setUploadSize(long uploadSize) {
        this.uploadSize = uploadSize;
        return this;
    }

    public String getCreateUser() {
        return createUser;
    }

    public BreakpointFile setCreateUser(String createUser) {
        this.createUser = createUser;
        return this;
    }

    public long getCreateTime() {
        return createTime;
    }

    public BreakpointFile setCreateTime(long createTime) {
        this.createTime = createTime;
        return this;
    }

    public String getUploadUser() {
        return uploadUser;
    }

    public BreakpointFile setUploadUser(String uploadUser) {
        this.uploadUser = uploadUser;
        return this;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public BreakpointFile setUploadTime(long uploadTime) {
        this.uploadTime = uploadTime;
        return this;
    }
}
