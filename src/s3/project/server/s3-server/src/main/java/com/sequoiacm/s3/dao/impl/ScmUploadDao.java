package com.sequoiacm.s3.dao.impl;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.metasource.*;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.dao.UploadDao;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository("UploadDao")
public class ScmUploadDao implements UploadDao {
    private static final Logger logger = LoggerFactory.getLogger(ScmUploadDao.class);

    private static final String tableName = S3CommonDefine.UPLOAD_META_TABLE_NAME;

    @Autowired
    MetaSourceService metaSourceService;

    @Override
    public void insertUploadMeta(TransactionContext transaction, UploadMeta meta)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName, transaction);

            BSONObject uploadMeta = convertMetaToBson(meta);
            cl.insert(uploadMeta);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "Insert upload failed. uploadId:" + meta.getUploadId(), e);
        }
    }

    @Override
    public void updateUploadMeta(TransactionContext transaction, UploadMeta update)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName, transaction);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(UploadMeta.META_BUCKET_ID, update.getBucketId());
            matcher.put(UploadMeta.META_KEY_NAME, update.getKey());
            matcher.put(UploadMeta.META_UPLOAD_ID, update.getUploadId());

            BSONObject updateMeta = convertMetaToBson(update);
            BSONObject setUpdate = new BasicBSONObject();
            setUpdate.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, updateMeta);

            BSONObject hint = new BasicBSONObject();
            hint.put("", "");

            cl.update(matcher, setUpdate, hint);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "Update upload by uploadId failed. uploadId:" + update.getUploadId(), e);
        }
    }

    @Override
    public UploadMeta queryUpload(Long bucketId, String objectName, long uploadId)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName);

            BSONObject matcher = new BasicBSONObject();
            if (bucketId != null) {
                matcher.put(UploadMeta.META_BUCKET_ID, bucketId);
            }
            if (objectName != null) {
                matcher.put(UploadMeta.META_KEY_NAME, objectName);
            }
            matcher.put(UploadMeta.META_UPLOAD_ID, uploadId);

            BSONObject queryResult = cl.queryOne(matcher, null, null);
            if (queryResult == null) {
                return null;
            }
            else {
                return new UploadMeta(queryResult);
            }
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "Query upload by uploadId failed. uploadId:" + uploadId, e);
        }
    }

    @Override
    public void deleteUploadByUploadId(TransactionContext transaction, long bucketId,
            String objectName, long uploadId) throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName, transaction);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(UploadMeta.META_BUCKET_ID, bucketId);
            matcher.put(UploadMeta.META_KEY_NAME, objectName);
            matcher.put(UploadMeta.META_UPLOAD_ID, uploadId);

            BSONObject hint = new BasicBSONObject();
            hint.put("", "");

            cl.delete(matcher, hint);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "Delete uploadMeta by uploadId failed. uploadId:" + uploadId, e);
        }
    }

    @Override
    public MetaCursor queryUploads(BSONObject statusMatcher, Long exceedTime)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName);

            BSONObject matcher = new BasicBSONObject();
            if (statusMatcher != null) {
                matcher.put(UploadMeta.META_STATUS, statusMatcher);
            }
            if (exceedTime != null) {
                BSONObject timeMatcher = new BasicBSONObject();
                timeMatcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_LT, exceedTime);
                matcher.put(UploadMeta.META_LAST_MODIFY_TIME, timeMatcher);
            }

            return cl.query(matcher, null, null);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "Query invalid uploads failed.", e);
        }
    }

    @Override
    public void initUploadMetaTable() throws S3ServerException {
        createUploadMetaTable();
    }

    private void createUploadMetaTable() throws S3ServerException {
        try {
            MetaAccessor uploadMetaAccessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.UPLOAD_META_TABLE_NAME);

            List<String> indexFiledList = new ArrayList<>();
            indexFiledList.add(UploadMeta.META_BUCKET_ID);
            indexFiledList.add(UploadMeta.META_KEY_NAME);
            indexFiledList.add(UploadMeta.META_UPLOAD_ID);

            IndexDef indexDef = new IndexDef();
            indexDef.setUnique(true);
            indexDef.setUnionKeys(indexFiledList);

            IndexDef uploadIdIndexDef = new IndexDef();
            uploadIdIndexDef.setUnique(false);
            uploadIdIndexDef.setUnionKeys(Arrays.asList(UploadMeta.META_UPLOAD_ID));

            uploadMetaAccessor.ensureTable(Arrays.asList(indexDef, uploadIdIndexDef));
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "create " + S3CommonDefine.UPLOAD_META_TABLE_NAME + " table failed.", e);
        }
    }

    private BSONObject convertMetaToBson(UploadMeta meta) {
        BSONObject uploadMeta = new BasicBSONObject();
        uploadMeta.put(UploadMeta.META_BUCKET_ID, meta.getBucketId());
        uploadMeta.put(UploadMeta.META_KEY_NAME, meta.getKey());
        uploadMeta.put(UploadMeta.META_UPLOAD_ID, meta.getUploadId());
        uploadMeta.put(UploadMeta.META_LAST_MODIFY_TIME, meta.getLastModified());
        uploadMeta.put(UploadMeta.META_SITE_ID, meta.getSiteId());
        uploadMeta.put(UploadMeta.META_SITE_TYPE, meta.getSiteType());
        uploadMeta.put(UploadMeta.META_DATA_ID, meta.getDataId());
        uploadMeta.put(UploadMeta.META_WORKSPACE, meta.getWsName());
        uploadMeta.put(UploadMeta.META_CACHE_CONTROL, meta.getCacheControl());
        uploadMeta.put(UploadMeta.META_CONTENT_DISPOSITION, meta.getContentDisposition());
        uploadMeta.put(UploadMeta.META_CONTENT_ENCODING, meta.getContentEncoding());
        uploadMeta.put(UploadMeta.META_CONTENT_LANGUAGE, meta.getContentLanguage());
        uploadMeta.put(UploadMeta.META_CONTENT_TYPE, meta.getContentType());
        uploadMeta.put(UploadMeta.META_EXPIRES, meta.getExpires());
        uploadMeta.put(UploadMeta.META_LIST, meta.getMetaList());
        uploadMeta.put(UploadMeta.META_STATUS, meta.getUploadStatus());
        uploadMeta.put(UploadMeta.META_WS_VERSION, meta.getWsVersion());

        return uploadMeta;
    }
}
