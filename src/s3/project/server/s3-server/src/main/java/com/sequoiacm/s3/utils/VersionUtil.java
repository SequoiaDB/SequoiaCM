package com.sequoiacm.s3.utils;

import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class VersionUtil {
    public static ScmVersion parseVersion(String versionId) throws S3ServerException {
        String[] versionArr = versionId.split("\\.");
        if (versionArr.length != 2) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_VERSION,
                    "versionId is invalid. versionId=" + versionId);
        }
        ScmVersion ret = new ScmVersion();
        try {
            ret.setMajorVersion(Integer.parseInt(versionArr[0]));
            ret.setMinorVersion(Integer.parseInt(versionArr[1]));
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.OBJECT_INVALID_VERSION,
                    "versionId is invalid. versionId=" + versionId, e);
        }
        return ret;
    }

    public static ScmVersion parseVersionIgnoreInvalidId(String versionId)
            throws S3ServerException {
        try {
            return parseVersion(versionId);
        }
        catch (S3ServerException e) {
            if (e.getError() == S3Error.OBJECT_INVALID_VERSION) {
                return null;
            }
            throw e;
        }
    }

}
