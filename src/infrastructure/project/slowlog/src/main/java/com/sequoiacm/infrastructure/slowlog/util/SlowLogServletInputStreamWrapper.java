package com.sequoiacm.infrastructure.slowlog.util;

import com.sequoiacm.infrastructure.slowlog.SlowLogManager;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;

public class SlowLogServletInputStreamWrapper extends ServletInputStream {

    private final ServletInputStream delegate;

    public SlowLogServletInputStreamWrapper(ServletInputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.readLine(b, off, len);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addReadClientSpend(System.currentTimeMillis() - start);
        }

    }

    @Override
    public boolean isFinished() {
        return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        delegate.setReadListener(readListener);
    }

    @Override
    public int read() throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.read();
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addReadClientSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.read(b);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addReadClientSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.read(b, off, len);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addReadClientSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long start = System.currentTimeMillis();
        try {
            return delegate.skip(n);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addReadClientSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
