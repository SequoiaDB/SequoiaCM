package com.sequoiacm.s3.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class ObjectUri {
    private String bucketName;
    private String objectName;
    private String versionId;

    public ObjectUri(String uri) throws S3ServerException {
        String decodeUri;
        try {
            decodeUri = URLDecoder.decode(uri, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.OBJECT_COPY_INVALID_SOURCE,
                    "Invalid source. source url = " + uri);
        }
        int beginBucket;
        if (decodeUri.startsWith(RestParamDefine.REST_DELIMITER)) {
            beginBucket = 1;
        }
        else {
            beginBucket = 0;
        }
        int beginObject = decodeUri.indexOf(RestParamDefine.REST_DELIMITER, beginBucket);
        if (beginObject == -1) {
            throw new S3ServerException(S3Error.OBJECT_COPY_INVALID_SOURCE,
                    "Invalid source. source url = " + uri);
        }

        this.bucketName = decodeUri.substring(beginBucket, beginObject);
        if (this.bucketName.length() == 0) {
            throw new S3ServerException(S3Error.OBJECT_COPY_INVALID_SOURCE,
                    "Invalid source. source url = " + uri);
        }

        int beginVersionId = decodeUri.indexOf(RestParamDefine.REST_SOURCE_VERSIONID, beginObject);
        if (beginVersionId == -1) {
            this.objectName = decodeUri.substring(beginObject + 1);
        }
        else {
            this.objectName = decodeUri.substring(beginObject + 1, beginVersionId);
            versionId = decodeUri
                    .substring(beginVersionId + RestParamDefine.REST_SOURCE_VERSIONID.length());
            if(versionId.equals(S3CommonDefine.NULL_VERSION_ID)){
                versionId = null;
            }
        }

        if (this.objectName.length() == 0) {
            throw new S3ServerException(S3Error.OBJECT_COPY_INVALID_SOURCE,
                    "Invalid source. source url = " + uri);
        }
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectName() {
        return objectName;
    }


    public String getVersionId() {
        return versionId;
    }
}
