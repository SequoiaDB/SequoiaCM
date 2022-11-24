package com.sequoiacm.s3.dao.impl;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.*;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.Part;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.dao.PartDao;
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

@Repository("PartDao")
public class ScmPartDao implements PartDao {
    private static final Logger logger = LoggerFactory.getLogger(ScmPartDao.class);

    private static final String tableName = S3CommonDefine.PARTS_TABLE_NAME;

    @Autowired
    MetaSourceService metaSourceService;

    @Override
    public void insertPart(TransactionContext transaction, Part part) throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName, transaction);

            BSONObject partMeta = convertMetaToBson(part);
            cl.insert(partMeta);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "insert part failed. uploadId:"
                    + part.getUploadId() + ", partNumber:" + part.getPartNumber(), e);
        }
    }

    @Override
    public void updatePart(TransactionContext transaction, Part part) throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName, transaction);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(Part.UPLOADID, part.getUploadId());
            matcher.put(Part.PARTNUMBER, part.getPartNumber());

            BSONObject updatePart = convertMetaToBson(part);
            BSONObject setUpdate = new BasicBSONObject();
            setUpdate.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, updatePart);

            BSONObject hint = new BasicBSONObject();
            hint.put("", "");

            cl.update(matcher, setUpdate, hint);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "update part failed. uploadId:"
                    + part.getUploadId() + ", partNumber:" + part.getPartNumber(), e);
        }
    }

    @Override
    public Part queryPart(long uploadId, long partNumber) throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(Part.UPLOADID, uploadId);
            matcher.put(Part.PARTNUMBER, partNumber);

            BSONObject result = cl.queryOne(matcher, null, null);
            if (result == null) {
                return null;
            }
            else {
                return new Part(result);
            }
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "query part failed. uploadId:" + uploadId + ", partNumber:" + partNumber, e);
        }
    }

    @Override
    public Part queryOnePart(long uploadId, Long size, Integer startNum, Integer endNum)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(Part.UPLOADID, uploadId);
            if (size != null) {
                matcher.put(Part.SIZE, size);
            }

            // valid part number: (1) - (10000)
            // reserved part number: (-1000) - (0)
            // available part number: (-1000) - (10000)
            // abandoned part number: () - (-1000)
            BSONObject partnumberMatcher = new BasicBSONObject();
            if (startNum != null) {
                partnumberMatcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_GTE, startNum);
            }
            if (endNum != null) {
                partnumberMatcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_LT, endNum);
            }

            matcher.put(Part.PARTNUMBER, partnumberMatcher);

            BSONObject order = new BasicBSONObject();
            order.put(Part.PARTNUMBER, 1);

            BSONObject result = cl.queryOne(matcher, null, order);
            if (result == null) {
                return null;
            }
            else {
                return new Part(result);
            }
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "query part by size failed. uploadId:" + uploadId + ", size:" + size, e);
        }
    }

    @Override
    public void deletePart(TransactionContext transaction, long uploadId, Long partNumber)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName, transaction);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(Part.UPLOADID, uploadId);
            if (partNumber != null) {
                matcher.put(Part.PARTNUMBER, partNumber);
            }

            BSONObject hint = new BasicBSONObject();
            hint.put("", "");

            cl.delete(matcher, hint);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "delete part failed. uploadId:" + uploadId + ", partNumber:" + partNumber, e);
        }
    }

    @Override
    public MetaCursor queryPartList(long uploadId, Integer start, Integer marker, Integer maxSize)
            throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            MetaAccessor cl = ms.createMetaAccessor(tableName);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(Part.UPLOADID, uploadId);
            BSONObject partNumberMatcher = new BasicBSONObject();
            if (start != null) {
                partNumberMatcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_GTE, start);
            }
            if (marker != null) {
                partNumberMatcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_GT, marker);
            }
            if (!partNumberMatcher.isEmpty()) {
                matcher.put(Part.PARTNUMBER, partNumberMatcher);
            }
            BSONObject isNull = new BasicBSONObject();
            isNull.put(SequoiadbHelper.SEQUOIADB_MATCHER_ISNULL, 0);
            matcher.put(Part.DATAID, isNull);

            BSONObject order = new BasicBSONObject();
            order.put(Part.PARTNUMBER, 1);

            long returnRow = -1L;
            if (maxSize != null) {
                returnRow = maxSize;
            }

            return cl.query(matcher, null, order, 0, returnRow);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "query part list failed. uploadId:" + uploadId + ", marker:" + marker, e);
        }
    }

    public void initPartsTable() throws S3ServerException {
        createPartsCL();
    }

    private void createPartsCL() throws S3ServerException {
        try {
            MetaAccessor partsAccessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.PARTS_TABLE_NAME);

            List<String> indexFiledList = new ArrayList<>();
            indexFiledList.add(Part.UPLOADID);
            indexFiledList.add(Part.PARTNUMBER);

            IndexDef indexDef = new IndexDef();
            indexDef.setUnique(true);
            indexDef.setUnionKeys(indexFiledList);
            partsAccessor.ensureTable(Arrays.asList(indexDef));
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "create " + S3CommonDefine.PARTS_TABLE_NAME + " table failed.", e);
        }
    }

    private BSONObject convertMetaToBson(Part part) {
        BSONObject partMeta = new BasicBSONObject();
        partMeta.put(Part.UPLOADID, part.getUploadId());
        partMeta.put(Part.PARTNUMBER, part.getPartNumber());
        partMeta.put(Part.DATA_CREATE_TIME, part.getDataCreateTime());
        partMeta.put(Part.DATAID, part.getDataId());
        partMeta.put(Part.SIZE, part.getSize());
        partMeta.put(Part.ETAG, part.getEtag());
        partMeta.put(Part.LASTMODIFIED, part.getLastModified());

        return partMeta;
    }
}
