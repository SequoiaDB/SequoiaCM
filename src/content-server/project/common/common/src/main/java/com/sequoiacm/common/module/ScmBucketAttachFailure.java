package com.sequoiacm.common.module;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.exception.ScmError;

public class ScmBucketAttachFailure {
    private final String fileId;
    private ScmError error;
    private String message;
    private BSONObject externalInfo;

    public ScmBucketAttachFailure(String fileId, ScmError error, String message,
            BSONObject externalInfo) {
        this.fileId = fileId;
        this.error = error;
        this.message = message;
        this.externalInfo = externalInfo;
        if (this.externalInfo == null) {
            this.externalInfo = new BasicBSONObject();
        }
    }

    public String getFileId() {
        return fileId;
    }

    public ScmError getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public BSONObject getExternalInfo() {
        return externalInfo;
    }

    @Override
    public String toString() {
        return "ScmBucketAttachFailure{" + "fileId='" + fileId + '\'' + ", error=" + error
                + ", message='" + message + '\'' + ", externalInfo=" + externalInfo + '}';
    }
}
