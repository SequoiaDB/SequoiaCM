package com.sequoiacm.client.dispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.common.CommonHelper;

public class CloseableFileDataEntity implements Closeable {
    private long dataLentgth;
    private InputStream dataIs;

    public CloseableFileDataEntity(long dataLength, InputStream dataIs) {
        this.dataLentgth = dataLength;
        this.dataIs = dataIs;
    }

    public long getLength() {
        return dataLentgth;
    }

    public int read(byte b[]) throws IOException {
        return dataIs.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException {
        return dataIs.read(b, off, len);
    }


    public int readAsMuchAsPossible(byte b[], int off, int len) throws IOException {
        return CommonHelper.readAsMuchAsPossible(dataIs, b, off, len);
    }

    public int readAsMuchAsPossible(byte b[]) throws IOException {
        return CommonHelper.readAsMuchAsPossible(dataIs, b);
    }

    @Override
    public void close() {
        ScmHelper.closeStream(dataIs);
    }

    public InputStream getDataIs() {
        return new ScmFileInputStream(this.dataIs, dataLentgth);
    }
}

class ScmFileInputStream extends InputStream {
    private final long dataLen;
    private long currentReadLen;
    private InputStream dataIs;

    public ScmFileInputStream(InputStream dataIs, long dataLen) {
        this.dataIs = dataIs;
        this.dataLen = dataLen;
    }

    @Override
    public int read() throws IOException {
        int ret = dataIs.read();
        incReadLenAndCheck(ret == -1 ? -1 : 1);
        return ret;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        int ret = CommonHelper.readAsMuchAsPossible(dataIs, b, off, len);
        incReadLenAndCheck(ret);
        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        long ret = dataIs.skip(n);
        incReadLenAndCheck(ret);
        return ret;
    }

    @Override
    public int available() throws IOException {
        return dataIs.available();
    }

    @Override
    public void close() throws IOException {
        dataIs.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void incReadLenAndCheck(long readLen) throws IOException {
        if (readLen == -1) {
            if (currentReadLen != dataLen) {
                throw new IOException(
                        "data is incomplete:expectLen=" + dataLen + ",actual=" + currentReadLen);
            }
            return;
        }
        currentReadLen += readLen;
    }
}