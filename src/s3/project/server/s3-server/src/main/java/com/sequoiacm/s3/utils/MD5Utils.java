package com.sequoiacm.s3.utils;

import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class MD5Utils {
    public static Boolean isMd5EqualWithETag(String contentMd5, String eTag)
            throws S3ServerException {
        try {
            if (contentMd5.length() % 4 != 0) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_DIGEST,
                        "decode md5 failed, contentMd5:" + contentMd5);
            }
            if (!Base64.isBase64(contentMd5)) {
                throw new S3ServerException(S3Error.OBJECT_INVALID_DIGEST,
                        "decode md5 failed, contentMd5:" + contentMd5);
            }
            String textMD5 = new String(Hex.encodeHex(Base64.decodeBase64(contentMd5)));
            if (textMD5.equals(eTag)) {
                return true;
            }
            else {
                return false;
            }
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_DIGEST,
                    "decode md5 failed, contentMd5:" + contentMd5, e);
        }
    }
}
