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

}
