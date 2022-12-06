package com.sequoiacm.s3.core;

import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.utils.CommonUtil;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class S3PutObjectRequest {

    private String md5;
    private InputStream objectData;
    private S3BasicObjectMeta objectMeta = new S3BasicObjectMeta();
    private long objectCreateTime = -1;

    public S3BasicObjectMeta getObjectMeta() {
        return objectMeta;
    }

    public void setObjectMeta(S3BasicObjectMeta objectMeta) {
        this.objectMeta = objectMeta;
    }

    public String getMd5() {
        return md5;
    }

    public void setObjectData(InputStream objectData) {
        this.objectData = objectData;
    }

    public InputStream getObjectData() {
        return objectData;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getObjectCreateTime() {
        return objectCreateTime;
    }

    public void setObjectCreateTime(long objectCreateTime) {
        this.objectCreateTime = objectCreateTime;
    }

    public S3PutObjectRequest(S3BasicObjectMeta meta, String md5, InputStream objectData) {
        this.objectMeta = meta;
        this.objectData = objectData;
        this.md5 = md5;
    }

    public S3PutObjectRequest(String bucketName, String objectKey,
            Map<String, String> requestHeaders, Map<String, String> xMeta, long realContenLength,
            String contentMD5, InputStream body) throws S3ServerException {
        objectMeta.setBucket(bucketName);
        objectMeta.setKey(objectKey);

        objectMeta
                .setCacheControl(requestHeaders.get(RestParamDefine.PutObjectHeader.CACHE_CONTROL));
        objectMeta.setContentDisposition(
                requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_DISPOSITION));
        objectMeta.setContentEncoding(
                requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_ENCODING));
        objectMeta.setContentType(requestHeaders.get(RestParamDefine.PutObjectHeader.CONTENT_TYPE));
        objectMeta.setExpires(requestHeaders.get(RestParamDefine.PutObjectHeader.EXPIRES));
        objectMeta.setContentLanguage(
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
        objectMeta.setMetaList(metaList);
        objectMeta.setSize(realContenLength);
        String taggingStr = requestHeaders.get(RestParamDefine.PutObjectHeader.X_AMZ_TAGGING);
        Map<String, String> tagMap = new HashMap<>();
        if (!StringUtils.isEmpty(taggingStr)) {
            tagMap = CommonUtil.parseObjectTagging(taggingStr);
        }
        objectMeta.setTagging(tagMap);
        String objectCreateTimeStr = requestHeaders
                .get(RestParamDefine.PutObjectHeader.X_SCM_OBJECT_CREATE_TIME);
        if (objectCreateTimeStr != null) {
            objectCreateTime = Long.parseLong(objectCreateTimeStr);
        }
        setMd5(contentMD5);
        setObjectData(body);
    }
}
