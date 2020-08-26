package com.sequoiacm.contentserver.common;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmServerException;

public class InputStreamWithCalcMd5 extends InputStream {
    private InputStream src;
    private MessageDigest md5Calc;
    private byte[] md5Bytes;

    public InputStreamWithCalcMd5(InputStream src) throws ScmServerException {
        this.src = src;
        this.md5Calc = ScmSystemUtils.createMd5Calc();
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
        len = src.read(b, off, len);
        if (len == -1) {
            return -1;
        }

        md5Calc.update(b, off, len);
        return len;
    }

    @Override
    public void close() throws IOException {
        src.close();
    }

    public String calcMd5() throws ScmSystemException {
        if (md5Bytes != null) {
            throw new ScmSystemException("can not calc md5 towice");
        }
        md5Bytes = md5Calc.digest();
        return DatatypeConverter.printBase64Binary(md5Bytes);
    }
}
