package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.checksum.ChecksumType;
import org.bson.BSONObject;

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
    private boolean isNeedMd5;
    private String md5;
    private BSONObject extraContext;

    public BreakpointFile setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public BreakpointFile setNeedMd5(boolean isNeedMd5) {
        this.isNeedMd5 = isNeedMd5;
        return this;
    }

    public boolean isNeedMd5() {
        return isNeedMd5;
    }

    public String getMd5() {
        return md5;
    }

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

    public BSONObject getExtraContext() {
        return extraContext;
    }

    public BreakpointFile setExtraContext(BSONObject extraContext) {
        this.extraContext = extraContext;
        return this;
    }

    @Override
    public String toString() {
        return "BreakpointFile: " + "workspaceName='" + workspaceName + '\'' + ", fileName='"
                + fileName + '\'' + ", siteId=" + siteId + ", siteName='" + siteName + '\''
                + ", checksumType=" + checksumType + ", checksum=" + checksum + ", dataId='"
                + dataId + '\'' + ", completed=" + completed + ", uploadSize=" + uploadSize
                + ", createUser='" + createUser + '\'' + ", createTime=" + createTime
                + ", uploadUser='" + uploadUser + '\'' + ", uploadTime=" + uploadTime
                + ", isNeedMd5=" + isNeedMd5 + ", md5='" + md5 + '\'' + ", extraContext="
                // 替换掉双引号，避免这个字符串拼接到JSON中时，出现解析不了的情况
                + extraContext.toString().replace("\"", "'");
    }
}
