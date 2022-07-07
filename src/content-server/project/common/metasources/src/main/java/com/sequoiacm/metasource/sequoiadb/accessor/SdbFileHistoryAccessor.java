package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.LruMap;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileAccessor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.exception.SDBError;

import java.util.Date;

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
            Date date) throws ScmMetasourceException {
        return baseAccesor.addToSiteList(fileId, majorVersion, minorVersion, siteId, date);
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
    public void createFileTable(BSONObject file) throws ScmMetasourceException {
        baseAccesor.createFileTable(file);
    }

    @Override
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject newFileInfo) throws ScmMetasourceException {
        try {
            BSONObject updator = new BasicBSONObject("$set", newFileInfo);
            BSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            return updateAndReturnNew(matcher, updator);
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
    public BSONObject updateFileExternalData(BSONObject matcher, BSONObject externalData)
            throws ScmMetasourceException {
        BSONObject newRecord = baseAccesor.updateFileExternalData(matcher, externalData);
        return compatibilityProcessor.fixHistoryRecordAndUpdateMeta(newRecord);
    }

    @Override
    public BSONObject queryAndDelete(BSONObject matcher, BSONObject orderby,
            BSONObject latestVersion) throws ScmMetasourceException {
        BSONObject ret = baseAccesor.queryAndDelete(matcher, orderby);
        return compatibilityProcessor.fixHistoryRecord(ret, latestVersion);
    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        baseAccesor.insert(insertor);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject orderBy, long skip, long limit)
            throws ScmMetasourceException {
        MetaCursor metaCursor = baseAccesor.query(matcher, null, orderBy, skip, limit);
        return new CompatibilityCursorWrapper(metaCursor, compatibilityProcessor);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject orderBy, BSONObject hint, long skip,
            long limit) throws ScmMetasourceException {
        MetaCursor metaCursor = baseAccesor.query(matcher, null, orderBy, hint, skip, limit, 0);
        return new CompatibilityCursorWrapper(metaCursor, compatibilityProcessor);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject orderBy) throws ScmMetasourceException {
        MetaCursor metaCursor = baseAccesor.query(matcher, null, orderBy);
        return new CompatibilityCursorWrapper(metaCursor, compatibilityProcessor);
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
        newHistoryRecord.putAll(latestVersionCache.getLatestVersionInfo());
        newHistoryRecord.putAll(historyRecord);
        historyFileAccessor.pureUpdate(historyRecord,
                new BasicBSONObject("$set", newHistoryRecord));
        return newHistoryRecord;
    }
}

class CompatibilityCursorWrapper implements MetaCursor {
    private final MetaCursor metaCursor;
    private final CompatibilityProcessor compatibilityProcessor;

    public CompatibilityCursorWrapper(MetaCursor metaCursor,
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
