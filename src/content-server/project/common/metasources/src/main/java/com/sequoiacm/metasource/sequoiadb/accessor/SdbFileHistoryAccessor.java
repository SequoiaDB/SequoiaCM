package com.sequoiacm.metasource.sequoiadb.accessor;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.LruMap;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.SDBError;

public class SdbFileHistoryAccessor implements MetaFileHistoryAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbFileHistoryAccessor.class);
    private final SdbFileBaseAccessor baseAccesor;
    private final CompatibilityProcessor compatibilityProcessor;

    public SdbFileHistoryAccessor(SdbMetaSourceLocation location, String wsName,
            SdbMetaSource metasource,
            String csName, String clName, TransactionContext context) {
        this.baseAccesor = new SdbFileBaseAccessor(location, metasource, csName, clName, context);
        this.compatibilityProcessor = new CompatibilityProcessor(wsName, this,
                metasource.getFileAccessor(location, wsName, context));
    }

    @Override
    public BSONObject delete(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException {
        return baseAccesor.delete(fileId, majorVersion, minorVersion);
    }

    @Override
    public void delete(String fileId) throws ScmMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(deletor, fileId);
            baseAccesor.delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete history file failed:table={}.{},fileId={}",
                    baseAccesor.getCsName(), baseAccesor.getClName(), fileId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + baseAccesor.getCsName() + "." + baseAccesor.getClName()
                            + ",fileId=" + fileId,
                    e);
        }
    }

    @Override
    public boolean addToSiteList(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date, int wsVersion, String tableName) throws ScmMetasourceException {
        return baseAccesor.addToSiteList(fileId, majorVersion, minorVersion, siteId, date,
                wsVersion, tableName);
    }

    @Override
    public boolean deleteNullFromSiteList(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException {
        return baseAccesor.deleteNullFromSiteList(fileId, majorVersion, minorVersion);
    }

    @Override
    public boolean updateAccessTime(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws ScmMetasourceException {
        return baseAccesor.updateAccessTime(fileId, majorVersion, minorVersion, siteId, date);
    }

    @Override
    public boolean updateAccessHistory(String fileId, int majorVersion, int minorVersion,
            int siteId, BasicBSONList newAccessTimeList)
            throws ScmMetasourceException {
        return baseAccesor.updateAccessHistory(fileId, majorVersion, minorVersion, siteId, newAccessTimeList);
    }

    @Override
    public boolean updateMd5(String fileId, int majorVersion, int minorVersion, String md5)
            throws ScmMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            // matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            BasicBSONObject updator = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_MD5, md5);
            updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, updator);
            return baseAccesor.updateAndCheck(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update md5 failed:table=" + baseAccesor.getCsName() + "."
                    + baseAccesor.getClName() + ",fileId="
                    + fileId + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion
                    + ",md5=" + md5);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update md5 failed:table=" + baseAccesor.getCsName() + "."
                            + baseAccesor.getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion + ",md5=" + md5,
                    e);
        }
    }

    @Override
    public boolean deleteFromSiteList(String fileId, int majorVersion, int minorVersion, int siteId)
            throws ScmMetasourceException {
        return baseAccesor.deleteFromSiteList(fileId, majorVersion, minorVersion, siteId);
    }

    @Override
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject updater) throws ScmMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            return updateAndReturnNew(matcher, updater);
        }
        catch (SdbMetasourceException e) {
            logger.error("updateFileInfo failed:table=" + baseAccesor.getCsName() + "."
                    + baseAccesor.getClName()
                    + ",fileId=" + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                    + minorVersion);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "updateFileInfo failed:table=" + baseAccesor.getCsName() + "."
                            + baseAccesor.getClName() + ",fileId="
                            + fileId + ",majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion,
                    e);
        }
    }

    @Override
    public boolean isIndexFieldExist(String fieldName) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = baseAccesor.getConnection();
            return SequoiadbHelper.isIndexFieldExist(sdb, fieldName, baseAccesor.getCsName(),
                    baseAccesor.getClName());
        }
        finally {
            if (sdb != null) {
                baseAccesor.releaseConnection(sdb);
            }
        }
    }

    @Override
    public BSONObject updateFileExternalData(BSONObject matcher, BSONObject externalData)
            throws ScmMetasourceException {
        BSONObject newRecord = baseAccesor.updateFileExternalData(matcher, externalData);
        return compatibilityProcessor.fixHistoryRecordAndUpdateMeta(newRecord);
    }

    @Override
    public BSONObject queryAndDelete(String fileId, BSONObject latestVersion,
            BSONObject additionalMatcher, BSONObject orderby) throws ScmMetasourceException {
        BSONObject ret = null;
        MetaCursor cursor = queryAndDeleteWithCursor(fileId, latestVersion, additionalMatcher,
                orderby);
        try {
            while (cursor.hasNext()) {
                ret = cursor.getNext();
            }
        }
        finally {
            cursor.close();
        }
        return ret;
    }

    @Override
    public MetaCursor queryAndDeleteWithCursor(String fileId, BSONObject latestVersion,
            BSONObject additionalMatcher, BSONObject orderby) throws ScmMetasourceException {

        BSONObject deletor = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(deletor, fileId);
        BasicBSONList andArr = new BasicBSONList();
        andArr.add(deletor);
        if (additionalMatcher != null) {
            andArr.add(additionalMatcher);
        }
        MetaCursor cursor = baseAccesor
                .queryAndDeleteWithCursor(new BasicBSONObject("$and", andArr), orderby);
        return new SpecifiedFileCursorWithFix(cursor, latestVersion, compatibilityProcessor);
    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        baseAccesor.insert(insertor);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject orderBy, long skip, long limit)
            throws ScmMetasourceException {
        MetaCursor metaCursor = baseAccesor.query(matcher, null, orderBy, skip, limit);
        return new CursorWithUpdateIncompleteRecord(metaCursor, compatibilityProcessor);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject orderBy, BSONObject hint, long skip,
            long limit) throws ScmMetasourceException {
        MetaCursor metaCursor = baseAccesor.query(matcher, null, orderBy, hint, skip, limit, 0);
        return new CursorWithUpdateIncompleteRecord(metaCursor, compatibilityProcessor);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject orderBy) throws ScmMetasourceException {
        MetaCursor metaCursor = baseAccesor.query(matcher, null, orderBy);
        return new CursorWithUpdateIncompleteRecord(metaCursor, compatibilityProcessor);
    }

    @Override
    public BSONObject queryOne(BSONObject matcher, BSONObject orderBy)
            throws ScmMetasourceException {
        BSONObject ret = baseAccesor.queryOne(matcher, null, orderBy);
        return compatibilityProcessor.fixHistoryRecordAndUpdateMeta(ret);
    }

    @Override
    public void delete(BSONObject deletor) throws ScmMetasourceException {
        baseAccesor.delete(deletor);
    }

    @Override
    public BSONObject updateAndReturnNew(BSONObject matcher, BSONObject updator)
            throws ScmMetasourceException {
        BSONObject ret = baseAccesor.queryAndUpdate(matcher, updator, null, true);
        return compatibilityProcessor.fixHistoryRecordAndUpdateMeta(ret);
    }

    @Override
    public MetaCursor queryAndUpdate(BSONObject matcher, BSONObject updater)
            throws ScmMetasourceException {
        MetaCursor cursor = baseAccesor.queryAndUpdateWithCursor(matcher, updater, null, true);
        return new CursorWithUpdateIncompleteRecord(cursor, compatibilityProcessor);
    }

    @Override
    public long count(BSONObject matcher) throws ScmMetasourceException {
        return baseAccesor.count(matcher);
    }

    @Override
    public double sum(BSONObject matcher, String field) throws ScmMetasourceException {
        return baseAccesor.sum(matcher, field);
    }

    void pureUpdate(BSONObject matcher, BSONObject updator) throws SdbMetasourceException {
        baseAccesor.update(matcher, updator);
    }
}

class LatestVersionCache {
    private BSONObject latestVersionInfo;
    private long fetchTime = System.currentTimeMillis();

    public LatestVersionCache(BSONObject latestVersionInfo) {
        this.latestVersionInfo = latestVersionInfo;
    }

    public BSONObject getLatestVersionInfo() {
        return latestVersionInfo;
    }

    public long getFetchTime() {
        return fetchTime;
    }
}

class CompatibilityProcessor {
    private static final Set<String> FIX_FIELD = new HashSet<>();
    static {
        FIX_FIELD.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_BATCH_ID);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_PROPERTIES);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_NAME);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_EXTRA_STATUS);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_TAGS);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_TYPE);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_INNER_UPDATE_USER);
        FIX_FIELD.add(FieldName.FIELD_CLFILE_INNER_USER);
    }
    private static final Logger logger = LoggerFactory.getLogger(CompatibilityProcessor.class);
    private final String wsName;
    private LruMap<String, LatestVersionCache> latestVersionCacheMap = new LruMap<>(100);
    private final SdbFileHistoryAccessor historyFileAccessor;
    private final MetaFileAccessor latestFileAccessor;

    public CompatibilityProcessor(String wsName, SdbFileHistoryAccessor historyFileAccessor,
            MetaFileAccessor latestFileAccessor) {
        this.historyFileAccessor = historyFileAccessor;
        this.latestFileAccessor = latestFileAccessor;
        this.wsName = wsName;
    }

    public BSONObject fixHistoryRecord(BSONObject historyRecord, BSONObject latestVersion) {
        if (historyRecord == null) {
            return null;
        }
        Object fileType = historyRecord.get(FieldName.FIELD_CLFILE_TYPE);
        if (fileType != null) {
            return historyRecord;
        }
        BSONObject ret = new BasicBSONObject();
        ret.putAll(latestVersion);
        ret.putAll(historyRecord);
        return ret;
    }

    public BSONObject fixHistoryRecordAndUpdateMeta(BSONObject historyRecord)
            throws ScmMetasourceException {
        if (historyRecord == null) {
            return null;
        }
        // 通过这个字段来鉴别这条记录是不是旧系统产生的历史文件，3.2版本之前历史文件仅包含少量字段
        Object fileType = historyRecord.get(FieldName.FIELD_CLFILE_TYPE);
        if (fileType != null) {
            return historyRecord;
        }

        String fileId = BsonUtils.getStringChecked(historyRecord, FieldName.FIELD_CLFILE_ID);

        LatestVersionCache latestVersionCache = latestVersionCacheMap.get(fileId);
        if (latestVersionCache == null
                || System.currentTimeMillis() - latestVersionCache.getFetchTime() > 5000) {
            String createMonth = BsonUtils.getStringChecked(historyRecord,
                    FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
            BasicBSONObject matcher = new BasicBSONObject();
            matcher.append(FieldName.FIELD_CLFILE_ID, fileId)
                    .append(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, createMonth);

            BSONObject latestVersion = latestFileAccessor.queryOne(matcher, null, null);
            if (latestVersion == null) {
                latestVersionCacheMap.remove(fileId);
                logger.warn(
                        "failed to fix history record, the latest version is not exist, return an uncompleted history record to caller: ws={}, fileId={}",
                        wsName, fileId);
                return historyRecord;
            }
            latestVersionCache = new LatestVersionCache(latestVersion);
            latestVersionCacheMap.put(fileId, latestVersionCache);
        }

        BasicBSONObject newHistoryRecord = new BasicBSONObject();
        fillLatestVersionProps(newHistoryRecord, latestVersionCache.getLatestVersionInfo());
        newHistoryRecord.putAll(historyRecord);
        historyFileAccessor.pureUpdate(historyRecord,
                new BasicBSONObject("$set", newHistoryRecord));
        return newHistoryRecord;
    }

    private void fillLatestVersionProps(BasicBSONObject newHistoryRecord,
            BSONObject latestVersionInfo) {
        // 使用最新版本的属性填充历史版本
        for (String field : FIX_FIELD) {
            newHistoryRecord.put(field, latestVersionInfo.get(field));
        }
    }
}

class SpecifiedFileCursorWithFix implements MetaCursor {
    private final MetaCursor specifiedFileCursor;
    private final CompatibilityProcessor compatibilityProcessor;
    private final BSONObject latestVersion;

    public SpecifiedFileCursorWithFix(MetaCursor specifiedFileCursor, BSONObject latestVersion,
            CompatibilityProcessor compatibilityProcessor) throws SdbMetasourceException {
        this.specifiedFileCursor = specifiedFileCursor;
        this.latestVersion = latestVersion;
        this.compatibilityProcessor = compatibilityProcessor;

    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return specifiedFileCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        return compatibilityProcessor.fixHistoryRecord(specifiedFileCursor.getNext(),
                latestVersion);
    }

    @Override
    public void close() {
        specifiedFileCursor.close();
    }
}

class CursorWithUpdateIncompleteRecord implements MetaCursor {
    private final MetaCursor metaCursor;
    private final CompatibilityProcessor compatibilityProcessor;

    public CursorWithUpdateIncompleteRecord(MetaCursor metaCursor,
            CompatibilityProcessor compatibilityProcessor) {
        this.metaCursor = metaCursor;
        this.compatibilityProcessor = compatibilityProcessor;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return metaCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        BSONObject historyRecord = metaCursor.getNext();
        return compatibilityProcessor.fixHistoryRecordAndUpdateMeta(historyRecord);
    }

    @Override
    public void close() {
        metaCursor.close();
    }
}
