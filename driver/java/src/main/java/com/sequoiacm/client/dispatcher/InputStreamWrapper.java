package com.sequoiacm.client.dispatcher;

import com.sequoiacm.client.util.ScmHelper;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamWrapper extends InputStream {

    private boolean closed = false;
    private final InputStream in;

    public InputStreamWrapper(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        ScmHelper.closeStream(in);
        closed = true;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public synchronized void mark(int readLimit) {
        in.mark(readLimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}