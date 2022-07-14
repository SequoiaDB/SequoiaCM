package com.sequoiacm.contentserver.common;

import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class InputStreamWithCalcMd5 extends InputStream {
    private boolean releaseInnerStreamWhenClose;
    private InputStream innerStream;
    private MessageDigest md5Calc;
    private byte[] md5Bytes;
    private long size = 0;

    public InputStreamWithCalcMd5(InputStream innerStream) throws ScmServerException {
        this(innerStream, true);
    }

    public InputStreamWithCalcMd5(InputStream innerStream, boolean releaseInnerStreamWhenClose)
            throws ScmServerException {
        this.innerStream = innerStream;
        this.md5Calc = ScmSystemUtils.createMd5Calc();
        this.releaseInnerStreamWhenClose = releaseInnerStreamWhenClose;
    }

    @Override
    public int read() throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        len = innerStream.read(b, off, len);
        if (len == -1) {
            return -1;
        }

        updateMd5(b, off, len);
        size += len;
        return len;
    }

    @SlowLog(operation = "calcMD5")
    private void updateMd5(byte[] b, int off, int len) {
        md5Calc.update(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (releaseInnerStreamWhenClose) {
            innerStream.close();
        }
    }

    @SlowLog(operation = "calcMD5")
    public String calcMd5() throws ScmSystemException {
        if (md5Bytes != null) {
            throw new ScmSystemException("can not calc md5 towice");
        }
        md5Bytes = md5Calc.digest();
        return DatatypeConverter.printBase64Binary(md5Bytes);
    }

    public long getSize() {
        return size;
    }
}
