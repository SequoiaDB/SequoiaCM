package com.sequoiacm.infrastructure.slowlog.util;

import com.sequoiacm.infrastructure.slowlog.SlowLogManager;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;

public class SlowLogServletOutputStreamWrapper extends ServletOutputStream {

    private final ServletOutputStream delegate;

    public SlowLogServletOutputStreamWrapper(ServletOutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        delegate.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void print(String s) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(s);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void print(boolean b) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(b);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }

    }

    @Override
    public void print(char c) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(c);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void print(int i) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(i);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void print(long l) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(l);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void print(float f) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(f);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void print(double d) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.print(d);

        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println() throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println();
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(String s) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(s);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(boolean b) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(b);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(char c) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(c);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(int i) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(i);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(long l) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(l);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(float f) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(f);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void println(double d) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.println(d);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.write(b);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.write(b, off, len);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void flush() throws IOException {
        long start = System.currentTimeMillis();
        try {
            delegate.flush();
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
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
