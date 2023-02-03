package com.sequoiacm.cephs3.dataoperation;

import static com.sequoiacm.metasource.MetaSourceDefine.CsName.CS_SCMSYSTEM;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.cephs3.lock.BucketNameLockPathFactory;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiacm.metasource.ScmMetasourceException;

/**
 * bucket name handle util class
 */
public class CephS3BucketManager {
    private static final Logger logger = LoggerFactory.getLogger(CephS3BucketManager.class);
    // key :workspace+ruleName, value:activeName
    private ConcurrentLruMap<String, String> activeBucketCache = ConcurrentLruMapFactory
            .create(200);
    private ScmLockManager lockManager;
    private MetaAccessor metaAccessor;
    private static CephS3BucketManager manager;

    public static CephS3BucketManager getInstance() {
        if (manager == null) {
            synchronized (CephS3BucketManager.class) {
                if (manager == null) {
                    manager = new CephS3BucketManager();
                }
            }
        }
        return manager;
    }

    private CephS3BucketManager() {
    }

    public void init(MetaSource metaSource, ScmLockManager lockManager)
            throws ScmMetasourceException {
        this.lockManager = lockManager;
        this.metaAccessor = getMetAccessor(metaSource);
    }

    private MetaAccessor getMetAccessor(MetaSource metaSource) throws ScmMetasourceException {
        return metaSource.createMetaAccessor(
                CS_SCMSYSTEM + "." + MetaSourceDefine.SystemClName.CL_DATA_BUCKET_NAME_ACTIVE);
    }

    public String getActiveBucketName(String workspaceName, String ruleBucketName)
            throws CephS3Exception {
        String activeBucketName = activeBucketCache.get(workspaceName + ruleBucketName);
        if (activeBucketName != null) {
            return activeBucketName;
        }

        BSONObject relClMatcher = new BasicBSONObject();
        relClMatcher.put(FieldName.FIELD_CL_WORKSPACE_NAME, workspaceName);
        relClMatcher.put(FieldName.FIELD_CL_BUCKET_RULE_NAME, ruleBucketName);
        BSONObject bucketInfo;
        try {
            bucketInfo = metaAccessor.queryOne(relClMatcher, null, null);
        }
        catch (ScmMetasourceException e) {
            throw new CephS3Exception("failed to query record, tableName = "
                    + MetaSourceDefine.SystemClName.CL_DATA_BUCKET_NAME_ACTIVE + ", matcher = "
                    + relClMatcher, e);
        }
        if (bucketInfo != null) {
            activeBucketName = (String) bucketInfo.get(FieldName.FIELD_CL_BUCKET_ACTIVE_NAME);
            activeBucketCache.put(workspaceName + ruleBucketName, activeBucketName);
            return activeBucketName;
        }

        activeBucketCache.put(workspaceName + ruleBucketName, ruleBucketName);
        return ruleBucketName;
    }

    private void insertAndAddCacheData(MetaAccessor metaAccessor, String ruleBucket,
            String activeBucket, String wsName) throws ScmMetasourceException {
        BSONObject insertor = new BasicBSONObject();
        insertor.put(FieldName.FIELD_CL_WORKSPACE_NAME, wsName);
        insertor.put(FieldName.FIELD_CL_BUCKET_RULE_NAME, ruleBucket);
        insertor.put(FieldName.FIELD_CL_BUCKET_ACTIVE_NAME, activeBucket);
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CL_WORKSPACE_NAME, wsName);
        matcher.put(FieldName.FIELD_CL_BUCKET_RULE_NAME, ruleBucket);
        metaAccessor.upsert(matcher, new BasicBSONObject("$set", insertor));
        activeBucketCache.put(wsName + ruleBucket, activeBucket);
    }
    // 返回值代表是否是新创建的桶
    public boolean createSpecifiedBucket(CephS3ConnWrapper conn, String bucketName)
            throws CephS3Exception {
        logger.info("create bucket: {}", bucketName);
        try {
            conn.createBucket(bucketName);
        }
        catch (CephS3Exception ex) {
            if (!ex.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_BUCKET_EXIST)) {
                throw ex;
            }
            logger.info("ignore create bucket, bucket already exist: {}", bucketName);
            return false;
        }
        return true;
    }

    public BucketCreateInfo createNewActiveBucket(CephS3ConnWrapper conn, String currentActiveBucketName,
            String ruleBucketName, String workspaceName, int siteId, CephS3DataService service)
            throws CephS3Exception {
        ScmLockPath lockPath = BucketNameLockPathFactory.createBucketNameLockPath(workspaceName,
                siteId);
        ScmLock lock = null;
        try {
            lock = lockManager.acquiresLock(lockPath);
            String currentActiveBucketNameInLock = getActiveBucketName(workspaceName,
                    ruleBucketName);
            if (!currentActiveBucketNameInLock.equals(currentActiveBucketName)) {
                logger.info(
                        "latest active bucket name is change, ignore create new active bucket: currentActiveBucket={}, latestActiveBucket={}, workspace={}",
                        currentActiveBucketName, currentActiveBucketNameInLock, workspaceName);
                return new BucketCreateInfo(currentActiveBucketNameInLock, false);
            }

            if (!conn.isObjectsUpToSpecifiedCount(currentActiveBucketNameInLock,
                    service.getQuotaExceededObjectThreshold())) {
                throw new CephS3Exception(
                        "failed to create new active bucket, the object count of current active bucket must be greater than "
                                + service.getQuotaExceededObjectThreshold() + ": workspace="
                                + workspaceName + ", currentActiveBucket="
                                + currentActiveBucketNameInLock);
            }

            String newActiveBucketName = generateNextActiveBucketName(currentActiveBucketNameInLock,
                    ruleBucketName);
            boolean isNewCreateBucket = createSpecifiedBucket(conn, newActiveBucketName);
            insertAndAddCacheData(metaAccessor, ruleBucketName, newActiveBucketName, workspaceName);
            return new BucketCreateInfo(newActiveBucketName, isNewCreateBucket);
        }
        catch (ScmLockException e) {
            throw new CephS3Exception("failed to lock bucketName, lockPath=" + lockPath.toString(),
                    e);
        }
        catch (ScmMetasourceException e) {
            throw new CephS3Exception("fail to upsert record, buckName = " + ruleBucketName, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
    // 根据工作区名删除对应缓存中和DATA_BUCKET_NAME_ACTIVE表中记录
    public void deleteActiveBucketMapping(String wsName) throws ScmDatasourceException {
        MetaCursor cursor = null;
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CL_WORKSPACE_NAME, wsName);
        try {
            cursor = metaAccessor.query(matcher, null, null);
            while (cursor.hasNext()) {
                String ruleBucketName = BsonUtils.getString(cursor.getNext(),
                        FieldName.FIELD_CL_BUCKET_RULE_NAME);
                activeBucketCache.remove(wsName + ruleBucketName);
            }
            metaAccessor.delete(matcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmDatasourceException("failed to delete bucket record, wsName=" + wsName, e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String generateNextActiveBucketName(String currentActiveBucketName, String ruleBucket) {
        if (!currentActiveBucketName.equals(ruleBucket)) {
            int value = Integer.parseInt(currentActiveBucketName
                    .substring(currentActiveBucketName.lastIndexOf("-") + 1));
            return ruleBucket + "-" + (value + 1);
        }
        else {
            return ruleBucket + "-1";
        }
    }
}
