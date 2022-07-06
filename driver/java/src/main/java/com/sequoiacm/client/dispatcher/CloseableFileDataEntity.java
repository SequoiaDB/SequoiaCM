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
        return new ScmFileInputStream(this.dataIs);
    }
}

class ScmFileInputStream extends InputStream {
    private InputStream dataIs;

    public ScmFileInputStream(InputStream dataIs) {
        this.dataIs = dataIs;
    }

    @Override
    public int read() throws IOException {
        return dataIs.read();
    }

    public int read(byte b[]) throws IOException {
        return CommonHelper.readAsMuchAsPossible(dataIs, b);
    }

    public int read(byte b[], int off, int len) throws IOException {
        return CommonHelper.readAsMuchAsPossible(dataIs, b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return dataIs.skip(n);
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
        dataIs.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        dataIs.reset();
    }

    @Override
    public boolean markSupported() {
        return dataIs.markSupported();
    }
}