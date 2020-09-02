package com.sequoiacm.s3.model;

import java.io.Closeable;
import java.io.InputStream;

import com.sequoiacm.s3.core.ObjectMeta;
import com.sequoiacm.s3.utils.CommonUtil;

public class GetResult implements Closeable {
    ObjectMeta meta;
    InputStream data;

    public GetResult(ObjectMeta meta, InputStream data) {
        this.meta = meta;
        this.data = data;
    }

    public void setMeta(ObjectMeta meta) {
        this.meta = meta;
    }

    public ObjectMeta getMeta() {
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
