package com.sequoiacm.s3import.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class InputStreamWithCalc extends InputStream {

    private InputStream src;
    private MessageDigest md5Calc;
    private byte[] md5Bytes;

    public InputStreamWithCalc(InputStream src) throws ScmToolsException {
        this.src = src;
        this.md5Calc = Md5Utils.createMd5Calc();
    }

    @Override
    public int read() throws IOException {
        throw new IOException("Unsupported");
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
        md5Bytes = md5Calc.digest();
        src.close();
    }

    public String getEtag() throws ScmToolsException {
        if (md5Bytes == null) {
            throw new ScmToolsException("inputstream did not close", S3ImportExitCode.SYSTEM_ERROR);
        }
        return new String(Hex.encodeHex(md5Bytes));
    }
}
