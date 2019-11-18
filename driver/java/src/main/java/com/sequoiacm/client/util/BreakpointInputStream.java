package com.sequoiacm.client.util;

import java.io.IOException;
import java.io.InputStream;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

public class BreakpointInputStream extends InputStream {
    private static final int DEFAULT_BREAKPOINT_SIZE = 1024 * 1024 * 2; // 2MB

    private final InputStream dataStream;
    private final int breakpointSize;
    private int readSize = 0;
    private boolean eof = false;

    public BreakpointInputStream(InputStream dataStream, int breakpointSize) throws ScmInvalidArgumentException {
        if (dataStream == null) {
            throw new ScmInvalidArgumentException("InputStream cannot be null");
        }

        if (breakpointSize <= 0) {
            throw new ScmInvalidArgumentException("BreakpointSize should be greater than zero");
        }

        this.dataStream = dataStream;
        this.breakpointSize = breakpointSize;
    }

    public BreakpointInputStream(InputStream dataStream) throws ScmInvalidArgumentException {
        this(dataStream, DEFAULT_BREAKPOINT_SIZE);
    }

    @Override
    public int read() throws IOException {
        if (readSize == breakpointSize || eof) {
            return -1;
        }

        int b =  dataStream.read();
        if (b == -1) {
            eof = true;
        } else {
            readSize += 1;
        }

        return b;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (readSize == breakpointSize || eof) {
            return -1;
        }
        if (len > (breakpointSize - readSize)) {
            len = breakpointSize - readSize;
        }

        int n = dataStream.read(b, off, len);
        if (n == -1) {
            eof = true;
        } else {
            readSize += n;
        }

        return n;
    }

    @Override
    public void close() throws IOException {
        //dataStream.close();
    }

    public boolean isEof() {
        return eof;
    }

    public void resetBreakpoint() {
        readSize = 0;
    }
}
