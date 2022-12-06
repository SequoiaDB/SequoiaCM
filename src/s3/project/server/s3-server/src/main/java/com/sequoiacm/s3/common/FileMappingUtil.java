package com.sequoiacm.s3.common;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
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

import java.util.HashMap;
import java.util.Map;

public class FileMappingUtil {
    public static final String CONTENT_LANGUAGE = "content_language";
    public static final String CONTENT_ENCODING = "content_encoding";
    public static final String CONTENT_DISPOSITION = "content_disposition";
    public static final String CACHE_CONTROL = "cache_control";
    public static final String EXPIRE = "expire";

    public static BSONObject buildFileInfo(S3BasicObjectMeta meta) {
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

    public static ListObjContent buildListObjContent(BSONObject content, String encodeType)
            throws S3ServerException {
        String key = BsonUtils.getStringChecked(content, FieldName.BucketFile.FILE_NAME);
        key = S3Codec.encode(key, encodeType);
        long updateTime = BsonUtils.getLongChecked(content, FieldName.BucketFile.FILE_UPDATE_TIME);
        String etag = BsonUtils.getStringChecked(content, FieldName.BucketFile.FILE_ETAG);
        String user = BsonUtils.getStringChecked(content, FieldName.BucketFile.FILE_CREATE_USER);
        long size = BsonUtils.getLongChecked(content, FieldName.BucketFile.FILE_SIZE);
        return new ListObjContent(key, DataFormatUtils.formatISO8601Date(updateTime), etag, size,
                new Owner(user, user));
    }

    public static ListObjContent buildListObjContent(S3ObjectMeta content, String encodeType)
            throws S3ServerException {
        String key = content.getKey();
        key = S3Codec.encode(key, encodeType);
        return new ListObjContent(key, DataFormatUtils.formatISO8601Date(content.getLastModified()),
                content.getEtag(), content.getSize(),
                new Owner(content.getUser(), content.getUser()));
    }

    public static ListDeleteMarker buildListDeleteMarker(S3ObjectMeta content, String encodeType,
            boolean isLatest) throws S3ServerException {
        String key = content.getKey();
        key = S3Codec.encode(key, encodeType);
        return new ListDeleteMarker(key, content.getVersionId(), isLatest,
                DataFormatUtils.formatISO8601Date(content.getLastModified()), content.getUser());
    }

    public static ListObjVersion buildListObjVersion(S3ObjectMeta content, String encodeType,
            boolean isLatest) throws S3ServerException {
        String key = content.getKey();
        key = S3Codec.encode(key, encodeType);
        return new ListObjVersion(key, content.getVersionId(), isLatest,
                DataFormatUtils.formatISO8601Date(content.getLastModified()), content.getUser(),
                content.getEtag(), content.getSize());
    }

    public static S3ObjectMeta buildS3ObjectMeta(String bucket, BSONObject fileInfo) {
        S3ObjectMeta ret = new S3ObjectMeta();
        ret.setUser(BsonUtils.getStringChecked(fileInfo, FieldName.FIELD_CLFILE_INNER_USER));
        ret.setBucket(bucket);
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
}
