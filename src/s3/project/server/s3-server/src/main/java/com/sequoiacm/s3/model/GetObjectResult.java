package com.sequoiacm.s3.model;

import java.io.Closeable;
import java.io.InputStream;

import com.sequoiacm.s3.core.S3ObjectMeta;
import com.sequoiacm.s3.utils.CommonUtil;

public class GetObjectResult implements Closeable {
    S3ObjectMeta meta;
    InputStream data;

    public GetObjectResult(S3ObjectMeta meta, InputStream data) {
        this.meta = meta;
        this.data = data;
    }

    public void setMeta(S3ObjectMeta meta) {
        this.meta = meta;
    }

    public S3ObjectMeta getMeta() {
        return meta;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public InputStream getData() {
        return data;
    }

    @Override
    public void close() {
        if (data != null) {
            CommonUtil.closeResource(data);
        }
    }
}
