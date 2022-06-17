package com.sequoiacm.s3import.common;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Utils {

    public static String getMD5(InputStream inputStream) throws ScmToolsException {
        try {
            MessageDigest md5 = createMd5Calc();
            byte[] buffer = new byte[64 * 1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(md5.digest()));
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to read stream", S3ImportExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(inputStream);
        }
    }

    public static MessageDigest createMd5Calc() throws ScmToolsException {
        try {
            return MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new ScmToolsException("Failed to get md5 message digest instance",
                    S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

    public static boolean isETagValid(String eTag) {
        return eTag.matches("^[A-Fa-f0-9]+$") && eTag.length() == 32;
    }
}
