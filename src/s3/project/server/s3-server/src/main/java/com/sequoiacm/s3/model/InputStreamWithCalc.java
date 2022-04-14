package com.sequoiacm.s3.model;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class InputStreamWithCalc extends InputStream {
    private InputStream src;
    private MessageDigest MD5;
    private long length;
    private byte[] md5Bytes;

    public InputStreamWithCalc(InputStream src) throws S3ServerException {
        this.src = src;
        try {
            this.MD5 = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "failed to get md5 digest instance",
                    e);
        }
    }

    @Override
    public int read() throws IOException {
        throw new RuntimeException("unsupport");
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

        MD5.update(b, off, len);
        length += len;
        return len;
    }

    @Override
    public void close() throws IOException {
        md5Bytes = MD5.digest();
        src.close();
    }

    public String getbase64md5() {
        if (md5Bytes == null) {
            throw new RuntimeException("inputstream did not close");
        }
        return SignUtil.base64(md5Bytes);
    }

    public String gethexmd5() {
        if (md5Bytes == null) {
            throw new RuntimeException("inputstream did not close");
        }
        return SignUtil.toHex(md5Bytes);
    }

    public long getLength() {
        return length;
    }

}
