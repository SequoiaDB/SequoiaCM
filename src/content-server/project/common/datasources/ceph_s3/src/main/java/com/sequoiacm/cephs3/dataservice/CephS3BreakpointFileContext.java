package com.sequoiacm.cephs3.dataservice;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class CephS3BreakpointFileContext {
    private static final String FIELD_UPLOAD_ID = "upload_id";
    private String uploadId;

    public CephS3BreakpointFileContext(String uploadId) {
        this.uploadId = uploadId;
    }

    public CephS3BreakpointFileContext(BSONObject context) {
        if (context != null) {
            this.uploadId = BsonUtils.getString(context, FIELD_UPLOAD_ID);
        }
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public BSONObject toBSON() {
        return new BasicBSONObject(FIELD_UPLOAD_ID, uploadId);
    }
}
