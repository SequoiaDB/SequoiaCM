package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.limiter.QuotaHelper;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaQuotaAccessor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncWriteTablePolicy implements WriteTablePolicy {
    private static final Logger logger = LoggerFactory.getLogger(SyncWriteTablePolicy.class);
    private MetaQuotaAccessor accessor;
    private ScmLockManager lockManager;
    private int quotaRoundNumber;
    private String bucketName;
    private volatile QuotaWrapper quotaUsedInfo;

    public SyncWriteTablePolicy(String bucketName, int quotaRoundNumber, long usedObjects,
            long usedSize) throws ScmServerException {
        this.accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getQuotaAccessor();
        this.lockManager = ScmLockManager.getInstance();
        this.quotaRoundNumber = quotaRoundNumber;
        this.bucketName = bucketName;
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        logger.info("SyncWriteTablePolicy init, bucketName: {}, quotaRoundNumber: {}", bucketName,
                quotaRoundNumber);
    }

    @Override
    public void acquireQuota(long acquireObjects, long acquireSize, long maxObjects, long maxSize,
            long createTime) throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory
                .createQuotaUsedLockPath(BucketQuotaManager.QUOTA_TYPE, bucketName);
        ScmLock lock = null;
        try {
            lock = lockManager.acquiresLock(lockPath);
            BSONObject record = accessor.getQuotaInfo(BucketQuotaManager.QUOTA_TYPE, bucketName);
            if (record == null) {
                logger.warn("quota record not found, bucketName: {}", bucketName);
                return;
            }

            long usedObjects = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_OBJECTS)
                    .longValue();
            long usedSize = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_SIZE)
                    .longValue();

            QuotaHelper.checkQuota(maxObjects, maxSize, acquireObjects, acquireSize, usedObjects,
                    usedSize, bucketName);
            BSONObject updator = buildUpdator(usedObjects + acquireObjects, usedSize + acquireSize);
            accessor.updateQuotaInfo(BucketQuotaManager.QUOTA_TYPE, bucketName, quotaRoundNumber,
                    updator);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to acquire quota:bucketName=" + bucketName
                    + ", acquireObjects=" + acquireObjects + ", acquireSize=" + acquireSize, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private BSONObject buildUpdator(long usedObjects, long usedSize) {
        BSONObject updator = new BasicBSONObject();
        updator.put(FieldName.Quota.USED_OBJECTS, Math.max(usedObjects, 0L));
        updator.put(FieldName.Quota.USED_SIZE, Math.max(usedSize, 0L));
        return updator;
    }

    @Override
    public void releaseQuota(long objects, long size, long createTime) throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory
                .createQuotaUsedLockPath(BucketQuotaManager.QUOTA_TYPE, bucketName);
        ScmLock lock = null;
        try {
            lock = lockManager.acquiresLock(lockPath);
            BSONObject record = accessor.getQuotaInfo(BucketQuotaManager.QUOTA_TYPE, bucketName);
            if (record == null) {
                logger.warn("quota record not found, bucketName: {}, quotaRoundNumber: {}",
                        bucketName, quotaRoundNumber);
                return;
            }
            long usedObjects = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_OBJECTS)
                    .longValue();
            long usedSize = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_SIZE)
                    .longValue();
            BSONObject updator = buildUpdator(usedObjects - objects, usedSize - size);
            accessor.updateQuotaInfo(BucketQuotaManager.QUOTA_TYPE, bucketName, quotaRoundNumber,
                    updator);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to release quota:bucketName=" + bucketName
                    + ", objects=" + objects + ", size=" + size, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void flush(boolean force) {
        // nothing to do
    }

    @Override
    public String getName() {
        return QuotaLimitConfig.POLICY_SYNC;
    }

    @Override
    public long getUsedObjectsCache() {
        return 0;
    }

    @Override
    public long getUsedSizeCache() {
        return 0;
    }

    @Override
    public QuotaWrapper getQuotaUsedInfo() {
        return quotaUsedInfo;
    }

    @Override
    public void setUsedQuota(long usedObjects, long usedSize) {
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
    }

}
