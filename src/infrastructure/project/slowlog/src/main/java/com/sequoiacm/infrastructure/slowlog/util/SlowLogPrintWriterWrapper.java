package com.sequoiacm.infrastructure.slowlog.util;

import com.sequoiacm.infrastructure.slowlog.SlowLogManager;

import java.io.PrintWriter;
import java.io.Writer;

public class SlowLogPrintWriterWrapper extends PrintWriter {

    public SlowLogPrintWriterWrapper(Writer out) {
        super(out);
    }

    @Override
    public void write(int c) {
        long start = System.currentTimeMillis();
        try {
            super.write(c);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        long start = System.currentTimeMillis();
        try {
            super.write(buf, off, len);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

    @Override
    public void write(String s, int off, int len) {
        long start = System.currentTimeMillis();
        try {
            super.write(s, off, len);
        }
        finally {
            SlowLogManager.getCurrentContext()
                    .addWriteResponseSpend(System.currentTimeMillis() - start);
        }
    }

}
