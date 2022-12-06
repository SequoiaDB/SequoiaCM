package com.sequoiacm.s3.core;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CopyObjectMatcher;
import com.sequoiacm.s3.model.ObjectUri;
import com.sequoiacm.s3.utils.CommonUtil;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class CopyObjectRequest {
    private CopyObjectMatcher sourceObjectMatcher;
    private S3BasicObjectMeta destObjectMeta = null;
    private boolean useSourceObjectMeta = false;
    private Map<String, String> newObjectTagging;
    private boolean useSourceObjectTagging = false;
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

    public Map<String, String> getNewObjectTagging() {
        return newObjectTagging;
    }

    public void setNewObjectTagging(Map<String, String> newObjectTagging) {
        this.newObjectTagging = newObjectTagging;
    }

    public boolean isUseSourceObjectTagging() {
        return useSourceObjectTagging;
    }

    public void setUseSourceObjectTagging(boolean useSourceObjectTagging) {
        this.useSourceObjectTagging = useSourceObjectTagging;
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
            ObjectUri sourceObjectUri, boolean useSourceObjectMeta, boolean useSourceObjectTagging,
            CopyObjectMatcher matcher) throws S3ServerException {
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

        String taggingStr = requestHeaders.get(RestParamDefine.PutObjectHeader.X_AMZ_TAGGING);
        Map<String, String> tagMap = new HashMap<>();
        if (!StringUtils.isEmpty(taggingStr)) {
            tagMap = CommonUtil.parseObjectTagging(taggingStr);
        }
        this.newObjectTagging = tagMap;

        sourceObjectBucket = sourceObjectUri.getBucketName();
        sourceObjectKey = sourceObjectUri.getObjectName();
        sourceObjectVersion = sourceObjectUri.getVersionId();

        this.sourceObjectMatcher = matcher;
        this.useSourceObjectMeta = useSourceObjectMeta;
        this.useSourceObjectTagging = useSourceObjectTagging;
    }

    @Override
    public String toString() {
        return "CopyObjectRequest{" + "sourceObjectMatcher=" + sourceObjectMatcher
                + ", destObjectMeta=" + destObjectMeta + ", useSourceObjectMeta="
                + useSourceObjectMeta + ", newObjectTagging=" + newObjectTagging
                + ", useSourceObjectTagging=" + useSourceObjectTagging + ", sourceObjectBucket='"
                + sourceObjectBucket + '\'' + ", sourceObjectKey='" + sourceObjectKey + '\''
                + ", sourceObjectVersion='" + sourceObjectVersion + '\'' + '}';
    }
}
