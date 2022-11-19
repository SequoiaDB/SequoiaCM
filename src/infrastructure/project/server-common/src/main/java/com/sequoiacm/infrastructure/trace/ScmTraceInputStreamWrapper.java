package com.sequoiacm.infrastructure.trace;

import org.springframework.cloud.sleuth.Span;

import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * 在流 close 时登记 sra(ServerReceivedAll)事件；
 * 
 * @see com.sequoiacm.infrastructure.trace.ScmTraceInputStreamWrapper#close()
 */
public class ScmTraceInputStreamWrapper extends ServletInputStream {

    private final ServletInputStream delegate;

    private final Span span;

    private boolean isEventRecorded = false;

    public ScmTraceInputStreamWrapper(ServletInputStream delegate, Span span) {
        this.delegate = delegate;
        this.span = span;
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        return delegate.readLine(b, off, len);
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
        return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        if (!isEventRecorded && span != null && span.isExportable()) {
            span.logEvent(ScmSpanLogEvenDefine.SERVER_RECEIVED_ALL);
            isEventRecorded = true;
        }

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
