package com.sequoiacm.cephs3.dataservice;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

public class CephS3BreakpointFileContext {
    private static final String FIELD_UPLOAD_ID = "upload_id";
    private BSONObject bsonContext;

    public CephS3BreakpointFileContext() {
        this.bsonContext = new BasicBSONObject();
    }

    public CephS3BreakpointFileContext(BSONObject context) {
        this.bsonContext = context;
    }

    public String getUploadId() {
        return BsonUtils.getString(bsonContext, FIELD_UPLOAD_ID);
    }

    public void setUploadId(String uploadId) {
        bsonContext.put(FIELD_UPLOAD_ID, uploadId);
    }

    public BSONObject getBSON() {
        return bsonContext;
    }
}
