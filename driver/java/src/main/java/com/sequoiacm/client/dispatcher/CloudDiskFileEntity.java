package com.sequoiacm.client.dispatcher;

import java.io.Closeable;
import java.io.InputStream;

import com.sequoiacm.client.util.ScmHelper;

public class CloudDiskFileEntity implements Closeable {
    private InputStream data;
    private long fileLength;

    public CloudDiskFileEntity(InputStream data, long fileLength) {
        this.data = data;
        this.fileLength = fileLength;
    }

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    @Override
    public void close() {
        ScmHelper.closeStream(data);
    }
}
