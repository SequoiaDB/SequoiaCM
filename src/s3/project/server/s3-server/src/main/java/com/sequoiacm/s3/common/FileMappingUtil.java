package com.sequoiacm.s3.common;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.core.S3BasicObjectMeta;
import com.sequoiacm.s3.core.S3ObjectMeta;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ListObjContent;
import com.sequoiacm.s3.model.ListObjVersion;
import com.sequoiacm.s3.model.Owner;
import com.sequoiacm.s3.model.ListDeleteMarker;
import com.sequoiacm.s3.utils.DataFormatUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FileMappingUtil {
    public static final String CONTENT_LANGUAGE = "content_language";
    public static final String CONTENT_ENCODING = "content_encoding";
    public static final String CONTENT_DISPOSITION = "content_disposition";
    public static final String CACHE_CONTROL = "cache_control";
    public static final String EXPIRE = "expire";


    public BSONObject buildFileInfo(S3BasicObjectMeta meta) {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FieldName.FIELD_CLFILE_NAME, meta.getKey());
        ret.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, meta.getContentType());
        BasicBSONObject extData = new BasicBSONObject();
        extData.put(FileMappingUtil.CACHE_CONTROL, meta.getCacheControl());
        extData.put(FileMappingUtil.CONTENT_DISPOSITION, meta.getContentDisposition());
        extData.put(FileMappingUtil.CONTENT_ENCODING, meta.getContentEncoding());
        extData.put(FileMappingUtil.CONTENT_LANGUAGE, meta.getContentLanguage());
        extData.put(FileMappingUtil.EXPIRE, meta.getExpires());
        ret.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA, extData);
        BasicBSONObject metaData = new BasicBSONObject(meta.getMetaList());
        ret.put(FieldName.FIELD_CLFILE_CUSTOM_METADATA, metaData);
        ret.put(FieldName.FIELD_CLFILE_FILE_TITLE, "");
        BasicBSONObject customTag = new BasicBSONObject(meta.getTagging());
        ret.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, customTag);
        return ret;
    }

    public ListObjContent buildListObjContent(BSONObject content, String encodeType)
            throws S3ServerException {
        S3ObjectSysMeta meta = new S3ObjectSysMeta(content);
        String key = S3Codec.encode(meta.key, encodeType);
        return new ListObjContent(key, DataFormatUtils.formatISO8601Date(meta.lastModified),
                meta.etag, meta.size, new Owner(meta.user, meta.user));
    }

    public ListDeleteMarker buildListDeleteMarker(BSONObject content, String encodeType,
            boolean isLatest) throws S3ServerException {
        S3ObjectSysMeta meta = new S3ObjectSysMeta(content);
        String key = S3Codec.encode(meta.key, encodeType);
        return new ListDeleteMarker(key, meta.versionId, isLatest,
                DataFormatUtils.formatISO8601Date(meta.lastModified), meta.user);
    }

    public ListObjVersion buildListObjVersion(BSONObject content, String encodeType,
            boolean isLatest) throws S3ServerException {
        S3ObjectSysMeta meta = new S3ObjectSysMeta(content);
        String key = S3Codec.encode(meta.key, encodeType);
        return new ListObjVersion(key, meta.versionId, isLatest,
                DataFormatUtils.formatISO8601Date(meta.lastModified), meta.user, meta.etag,
                meta.size);
    }

    public String mapS3VersionId(int fileMajorVersion, int fileMinorVersion) {
        if (fileMinorVersion == CommonDefine.File.NULL_VERSION_MINOR
                && fileMajorVersion == CommonDefine.File.NULL_VERSION_MAJOR) {
            return "null";
        }
        return String.format("%d.%d", fileMajorVersion, fileMinorVersion);
    }

    public S3ObjectMeta buildS3ObjectMeta(Bucket bucket, FileMeta fileMeta)
            throws ScmServerException {
        BSONObject fileInfo = fileMeta.toUserInfoBSON();
        S3ObjectMeta ret = new S3ObjectMeta();
        ret.setUser(BsonUtils.getStringChecked(fileInfo, FieldName.FIELD_CLFILE_INNER_USER));
        ret.setBucket(bucket.getBucketName());
        ret.setKey(BsonUtils.getStringChecked(fileInfo, FieldName.FIELD_CLFILE_NAME));
        if (ScmFileVersionHelper.isSpecifiedVersion(fileInfo, CommonDefine.File.NULL_VERSION_MAJOR,
                CommonDefine.File.NULL_VERSION_MINOR)) {
            ret.setVersionId("null");
        }
        else {
            ret.setVersionId(fileInfo.get(FieldName.FIELD_CLFILE_MAJOR_VERSION) + "."
                    + fileInfo.get(FieldName.FIELD_CLFILE_MINOR_VERSION));
        }
        ret.setDeleteMarker(
                BsonUtils.getBooleanOrElse(fileInfo, FieldName.FIELD_CLFILE_DELETE_MARKER, false));
        ret.setSize(
                BsonUtils.getNumberChecked(fileInfo, FieldName.FIELD_CLFILE_FILE_SIZE).longValue());

        Map<String, String> metalist = new HashMap<>();
        BSONObject customMeta = BsonUtils.getBSON(fileInfo, FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        if (customMeta != null) {
            for (String key : customMeta.keySet()) {
                metalist.put(key, (String) customMeta.get(key));
            }
        }
        ret.setMetaList(metalist);

        Map<String, String> tagging = new HashMap<>();
        BSONObject customTag = BsonUtils.getBSON(fileInfo, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        if (customTag != null) {
            for (String key : customTag.keySet()) {
                tagging.put(key, (String) customTag.get(key));
            }
        }
        ret.setTagging(tagging);

        BSONObject externalData = BsonUtils.getBSON(fileInfo,
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        if (externalData != null) {
            ret.setCacheControl(BsonUtils.getString(externalData, FileMappingUtil.CACHE_CONTROL));
            ret.setContentEncoding(
                    BsonUtils.getString(externalData, FileMappingUtil.CONTENT_ENCODING));
            ret.setContentDisposition(
                    BsonUtils.getString(externalData, FileMappingUtil.CONTENT_DISPOSITION));
            ret.setExpires(BsonUtils.getString(externalData, FileMappingUtil.EXPIRE));
            ret.setContentLanguage(
                    BsonUtils.getString(externalData, FileMappingUtil.CONTENT_LANGUAGE));
        }
        ret.setContentType(BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_FILE_MIME_TYPE));

        String md5 = BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_FILE_MD5);
        if (md5 != null) {
            ret.setEtag(SignUtil.toHex(md5));
        }
        else {
            ret.setEtag(BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_FILE_ETAG));
        }
        ret.setLastModified(BsonUtils
                .getNumberChecked(fileInfo, FieldName.FIELD_CLFILE_INNER_UPDATE_TIME).longValue());
        return ret;
    }

    private class S3ObjectSysMeta {
        private String etag;
        private String versionId;
        private long lastModified;
        private long size;
        private String user;
        private String key;

        private S3ObjectSysMeta(BSONObject fileRecord) {
            key = BsonUtils.getStringChecked(fileRecord, FieldName.BucketFile.FILE_NAME);
            lastModified = BsonUtils.getLongChecked(fileRecord,
                    FieldName.BucketFile.FILE_UPDATE_TIME);
            etag = BsonUtils.getString(fileRecord, FieldName.BucketFile.FILE_ETAG);
            user = BsonUtils.getStringChecked(fileRecord, FieldName.BucketFile.FILE_CREATE_USER);
            size = BsonUtils.getLongChecked(fileRecord, FieldName.BucketFile.FILE_SIZE);
            if (ScmFileVersionHelper.isSpecifiedVersion(fileRecord,
                    CommonDefine.File.NULL_VERSION_MAJOR, CommonDefine.File.NULL_VERSION_MINOR)) {
                versionId = "null";
            }
            else {
                versionId = fileRecord.get(FieldName.FIELD_CLFILE_MAJOR_VERSION) + "."
                        + fileRecord.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
            }
        }
    }
}
