package com.sequoiacm.om.omserver.common;

import java.io.IOException;
import java.io.InputStream;

public class ScmOmInputStream extends InputStream {

    private InputStream is;
    private boolean isClosed;

    public ScmOmInputStream(InputStream is) {
        this.is = is;
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            CommonUtil.closeResource(is);
            isClosed = true;
        }
    }

    public boolean isClosed() {
        return isClosed;
    }
}
