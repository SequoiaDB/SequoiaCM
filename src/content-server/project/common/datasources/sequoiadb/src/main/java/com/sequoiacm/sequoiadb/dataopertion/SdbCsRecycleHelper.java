package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.lock.ScmLockPathFactory;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.common.CommonDefine;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiacm.metasource.sequoiadb.module.ScmRecyclingLog;
import com.sequoiacm.sequoiadb.dataservice.SequoiadbHelper;
import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SdbCsRecycleHelper {
    private static final Logger logger = LoggerFactory.getLogger(SdbCsRecycleHelper.class);

    // true: 删除成功
    // false: 集合空间不为空或集合空间不存在
    public static boolean deleteCsIfEmpty(String csName, String siteName, SdbDataService service,
            MetaDataOperator operator, ScmLockManager lockManager) throws SequoiadbException,
            ScmMetasourceException, ScmServerException, ScmLockException {
        Sequoiadb sequoiadb = service.getSequoiadb();
        try {
            if (sequoiadb.isCollectionSpaceExist(csName)) {
                if (hasRecordOrLob(sequoiadb.getCollectionSpace(csName))) {
                    return false;
                }
                else {
                    ScmLock scmLock = null;
                    try {
                        scmLock = lockCs(lockManager, csName, siteName);
                        return doDropCS(csName, service, operator, sequoiadb);
                    }
                    finally {
                        if (scmLock != null) {
                            scmLock.unlock();
                        }
                    }

                }
            }
            else {
                // 集合空间不存在，查记录表，检查重命名后的cs是否存在
                String renamedCsName = queryRenamedCs(csName, operator);
                if (renamedCsName == null) {
                    return false;
                }
                ScmLock scmLock = null;
                try {
                    scmLock = lockCs(lockManager, csName, siteName);
                    return redoDropCs(csName, operator, sequoiadb, renamedCsName);
                }
                finally {
                    if (scmLock != null) {
                        scmLock.unlock();
                    }
                }
            }
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()) {
                return false;
            }
            else {
                throw e;
            }
        }
        finally {
            service.releaseSequoiadb(sequoiadb);
        }
    }

    private static boolean redoDropCs(String csName, MetaDataOperator operator, Sequoiadb sequoiadb,
            String renamedCsName) throws ScmMetasourceException {
        if (sequoiadb.isCollectionSpaceExist(renamedCsName)) {
            if (hasRecordOrLob(sequoiadb.getCollectionSpace(renamedCsName))) {
                // 撤销重命名
                cancelRenameCs(sequoiadb, csName, renamedCsName, operator);
                return false;
            }
            else {
                dropRenamedCs(sequoiadb, csName, renamedCsName, operator);
                return true;
            }
        }
        else {
            removeRecyclingLogSilence(csName, operator);
            return false;
        }
    }

    private static boolean doDropCS(String csName, SdbDataService service,
            MetaDataOperator operator, Sequoiadb sequoiadb)
            throws ScmMetasourceException, ScmServerException {
        if (sequoiadb.isCollectionSpaceExist(csName)) {
            String newCsName = csName + "_SYS_BAK_" + new Date().getTime();
            recordRecyclingLog(sequoiadb, csName, newCsName, service, operator);
            // 重命名cs
            logger.info("renaming collection space, oldCsName={}, newCsName={}", csName, newCsName);
            sequoiadb.renameCollectionSpace(csName, newCsName);
            // 检查重命名后的cs是否还有数据
            if (hasRecordOrLob(sequoiadb.getCollectionSpace(newCsName))) {
                // 撤销重命名
                cancelRenameCs(sequoiadb, csName, newCsName, operator);
                return false;
            }
            else {
                // 尝试删除重命名后的cs
                dropRenamedCs(sequoiadb, csName, newCsName, operator);
                return true;
            }
        }
        else {
            return false;
        }
    }

    private static ScmLock lockCs(ScmLockManager lockManager, String csName, String siteName)
            throws ScmLockException {
        ScmLockPath lockPath = ScmLockPathFactory.createDataTableLockPath(siteName, csName);
        return lockManager.acquiresLock(lockPath);
    }

    private static void cancelRenameCs(Sequoiadb sequoiadb, String csName, String renamedCsName,
            MetaDataOperator operator) {
        logger.info("cancelling rename collection space, csName={}, renamedCsName={}", csName,
                renamedCsName);
        sequoiadb.renameCollectionSpace(renamedCsName, csName);
        removeRecyclingLogSilence(csName, operator);
    }

    private static void dropRenamedCs(Sequoiadb sequoiadb, String oldCsName, String renamedCsName,
            MetaDataOperator operator) throws ScmMetasourceException {
        logger.info("dropping collection space, csName={}", renamedCsName);
        BSONObject options = new BasicBSONObject("SkipRecycleBin", true);
        sequoiadb.dropCollectionSpace(renamedCsName, options);
        removeRecyclingLogSilence(oldCsName, operator);
        operator.removeTableNameRecord(oldCsName);
    }

    public static String queryRenamedCs(String csName, MetaDataOperator operator)
            throws ScmMetasourceException {
        ScmRecyclingLog scmRecyclingLog = operator.queryRecyclingLog(csName);
        if (scmRecyclingLog == null) {
            return null;
        }
        return BsonUtils.getString(scmRecyclingLog.getLogInfo(),
                MetaDataOperator.FIELD_LOG_INFO_RENAMED_COLLECTION_SPACE);
    }

    public static void removeRecyclingLogSilence(String csName, MetaDataOperator operator) {
        try {
            operator.removeRecyclingLog(csName);
        }
        catch (Exception e) {
            logger.warn("failed to remove recycling log, csName={}", csName, e);
        }
    }

    private static void recordRecyclingLog(Sequoiadb sequoiadb, String oldCsName, String newCsName,
            SdbDataService service, MetaDataOperator operator)
            throws ScmMetasourceException, ScmServerException {
        // 登记前先把旧的数据删掉
        ScmRecyclingLog oldRecyclingLog = operator.queryRecyclingLog(oldCsName);
        if (oldRecyclingLog != null) {
            BSONObject logInfo = oldRecyclingLog.getLogInfo();
            if (logInfo != null) {
                String renamedCsName = (String) logInfo
                        .get(MetaDataOperator.FIELD_LOG_INFO_RENAMED_COLLECTION_SPACE);
                if (renamedCsName != null && sequoiadb.isCollectionSpaceExist(renamedCsName)) {
                    // should never happen
                    logger.error("exist residual collection space in space recycling, csName={}",
                            renamedCsName);
                    throw new ScmServerException(ScmError.SYSTEM_ERROR,
                            "exist residual collection space in space recycling, csName="
                                    + renamedCsName);
                }
            }
        }
        operator.removeRecyclingLog(oldCsName);

        ScmRecyclingLog scmRecyclingLog = new ScmRecyclingLog();
        scmRecyclingLog.setSiteId(service.getSiteId());
        scmRecyclingLog.setDataSourceType(CommonDefine.SPACE_RECYCLING_LOG_SEQUOIADB);
        scmRecyclingLog.setTime(formatDate(new Date()));
        BSONObject logInfo = new BasicBSONObject();
        logInfo.put(MetaDataOperator.FIELD_LOG_INFO_ORIGINAL_COLLECTION_SPACE, oldCsName);
        logInfo.put(MetaDataOperator.FIELD_LOG_INFO_RENAMED_COLLECTION_SPACE, newCsName);
        scmRecyclingLog.setLogInfo(logInfo);
        operator.insertRecyclingLog(scmRecyclingLog);

    }

    private static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date);
    }

    private static boolean hasRecordOrLob(CollectionSpace cs) {
        String csName = cs.getName();
        List<String> collectionNames = cs.getCollectionNames();
        for (String collectionName : collectionNames) {
            DBCollection collection = null;
            DBCursor dbCursor = null;
            try {
                collection = cs.getCollection(collectionName.substring(csName.length() + 1));
                BSONObject bsonObject = collection.queryOne();
                if (bsonObject != null) {
                    logger.info("collectionSpace contains record, csName={}", csName);
                    return true;
                }
                dbCursor = collection.listLobs();
                if (dbCursor.hasNext()) {
                    logger.info("collectionSpace contains lob, csName={}", csName);
                    return true;
                }
            }
            catch (BaseException e) {
                if (e.getErrorCode() != SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                    throw e;
                }
            }
            finally {
                if (dbCursor != null) {
                    dbCursor.close();
                }
            }
        }
        return false;
    }

    public static boolean recoverIfNeeded(Sequoiadb sdb, String csName, String siteName,
            MetaDataOperator metaDataOperator, ScmLockManager lockManager)
            throws SequoiadbException {
        ScmLock scmLock = null;
        try {
            scmLock = lockCs(lockManager, csName, siteName);
            return undoDropCS(sdb, csName, metaDataOperator);
        }
        catch (ScmLockException e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "failed to recover collection space,csName=" + csName, e);
        }
        finally {
            if (scmLock != null) {
                scmLock.unlock();
            }
        }
    }

    private static boolean undoDropCS(Sequoiadb sdb, String csName,
            MetaDataOperator metaDataOperator) throws SequoiadbException {
        try {
            if (sdb.isCollectionSpaceExist(csName)) {
                return false;
            }
            else {
                String renamedCs = queryRenamedCs(csName, metaDataOperator);
                if (renamedCs == null) {
                    return false;
                }
                if (sdb.isCollectionSpaceExist(renamedCs)) {
                    logger.info("recovering collection space: {} => {}", renamedCs, csName);
                    sdb.renameCollectionSpace(renamedCs, csName);
                    removeRecyclingLogSilence(csName, metaDataOperator);
                    return true;
                }
                else {
                    logger.info(
                            "renamed collection space is not exist, skip recover: csName={}, renamedCsName={}",
                            csName, renamedCs);
                    removeRecyclingLogSilence(csName, metaDataOperator);
                    return false;
                }
            }
        }
        catch (Exception e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "failed to recover collection space,csName=" + csName, e);
        }

    }

    public static boolean createCs(Sequoiadb sdb, String csName, String siteName,
            SdbDataLocation sdbLocation, ScmLockManager lockManager,
            MetaDataOperator metaDataOperator) throws SequoiadbException {
        ScmLock scmLock = null;
        try {
            scmLock = lockCs(lockManager, csName, siteName);
            if (sdb.isCollectionSpaceExist(csName)) {
                return false;
            }
            boolean recovered = undoDropCS(sdb, csName, metaDataOperator);
            if (!recovered) {
                return SequoiadbHelper.createCS(sdb, csName, sdbLocation);
            }
            else {
                return false;
            }
        }
        catch (ScmLockException e) {
            throw new SequoiadbException("failed to create collection space: csName=" + csName, e);
        }
        finally {
            if (scmLock != null) {
                scmLock.unlock();
            }
        }
    }
}
