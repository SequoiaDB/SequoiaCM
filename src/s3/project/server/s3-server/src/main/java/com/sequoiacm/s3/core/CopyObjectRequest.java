package com.sequoiacm.s3.core;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.model.CopyObjectMatcher;
import com.sequoiacm.s3.model.ObjectUri;

import java.util.HashMap;
import java.util.Map;

public class CopyObjectRequest {
    private CopyObjectMatcher sourceObjectMatcher;
    private S3BasicObjectMeta destObjectMeta = null;
    private boolean useSourceObjectMeta = false;
    private String sourceObjectBucket;
    private String sourceObjectKey;
    private String sourceObjectVersion;

    public CopyObjectMatcher getSourceObjectMatcher() {
        return sourceObjectMatcher;
    }

    public void setSourceObjectMatcher(CopyObjectMatcher sourceObjectMatcher) {
        this.sourceObjectMatcher = sourceObjectMatcher;
    }

    public S3BasicObjectMeta getDestObjectMeta() {
        return destObjectMeta;
    }

    public void setDestObjectMeta(S3BasicObjectMeta destObjectMeta) {
        this.destObjectMeta = destObjectMeta;
    }

    public boolean isUseSourceObjectMeta() {
        return useSourceObjectMeta;
    }

    public void setUseSourceObjectMeta(boolean useSourceObjectMeta) {
        this.useSourceObjectMeta = useSourceObjectMeta;
    }

    public String getSourceObjectBucket() {
        return sourceObjectBucket;
    }

    public void setSourceObjectBucket(String sourceObjectBucket) {
        this.sourceObjectBucket = sourceObjectBucket;
    }

    public String getSourceObjectKey() {
        return sourceObjectKey;
    }

    public void setSourceObjectKey(String sourceObjectKey) {
        this.sourceObjectKey = sourceObjectKey;
    }

    public String getSourceObjectVersion() {
        return sourceObjectVersion;
    }

    public void setSourceObjectVersion(String sourceObjectVersion) {
        this.sourceObjectVersion = sourceObjectVersion;
    }

    public CopyObjectRequest(String destBucketName, String destObjectKey,
            Map<String, String> requestHeaders, Map<String, String> xMeta,
            ObjectUri sourceObjectUri, boolean useSourceObjectMeta, CopyObjectMatcher matcher) {
        destObjectMeta = new S3BasicObjectMeta();
        destObjectMeta.setBucket(destBucketName);
        destObjectMeta.setKey(destObjectKey);
        destObjectMeta
                .setCacheControl(requestHeaders.get(RestParamDefine.PutObjectHeader.CACHE_CONTROL));
        destObjectMeta.setContentDisposition(
                requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_DISPOSITION));
        destObjectMeta.setContentEncoding(
                requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_ENCODING));
        destObjectMeta
                .setContentType(requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_TYPE));
        destObjectMeta.setExpires(requestHeaders.get(RestParamDefine.PutObjectHeader.EXPIRES));
        destObjectMeta.setContentLanguage(
                requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_LANGUAGE));

        HashMap<String, String> metaList = new HashMap<>();
        for (Map.Entry<String, String> entry : xMeta.entrySet()) {
            if (!entry.getKey().startsWith(RestParamDefine.PutObjectHeader.X_AMZ_META_PREFIX)) {
                metaList.put(entry.getKey(), entry.getValue());
                continue;
            }
            metaList.put(
                    entry.getKey()
                            .substring(RestParamDefine.PutObjectHeader.X_AMZ_META_PREFIX.length()),
                    entry.getValue());
        }
        destObjectMeta.setMetaList(metaList);

        sourceObjectBucket = sourceObjectUri.getBucketName();
        sourceObjectKey = sourceObjectUri.getObjectName();
        sourceObjectVersion = sourceObjectUri.getVersionId();

        this.sourceObjectMatcher = matcher;
        this.useSourceObjectMeta = useSourceObjectMeta;
    }

    @Override
    public String toString() {
        return "CopyObjectRequest{" + "sourceObjectMatcher=" + sourceObjectMatcher
                + ", destObjectMeta=" + destObjectMeta + ", useSourceObjectMeta="
                + useSourceObjectMeta + ", sourceObjectBucket='" + sourceObjectBucket + '\''
                + ", sourceObjectKey='" + sourceObjectKey + '\'' + ", sourceObjectVersion='"
                + sourceObjectVersion + '\'' + '}';
    }
}
