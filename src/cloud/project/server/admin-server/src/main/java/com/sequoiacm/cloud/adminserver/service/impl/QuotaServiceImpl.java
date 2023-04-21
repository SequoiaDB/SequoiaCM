package com.sequoiacm.cloud.adminserver.service.impl;

import com.sequoiacm.cloud.adminserver.config.QuotaSyncConfig;
import com.sequoiacm.cloud.adminserver.core.QuotaHelper;
import com.sequoiacm.cloud.adminserver.core.QuotaSyncMsgSender;
import com.sequoiacm.cloud.adminserver.dao.QuotaConfigDao;
import com.sequoiacm.cloud.adminserver.dao.QuotaSyncDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.lock.LockPathFactory;
import com.sequoiacm.cloud.adminserver.model.QuotaConfigDetail;
import com.sequoiacm.cloud.adminserver.model.QuotaResult;
import com.sequoiacm.cloud.adminserver.model.QuotaSyncInfo;
import com.sequoiacm.cloud.adminserver.service.QuotaService;
import com.sequoiacm.cloud.adminserver.service.QuotaSyncService;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaUpdator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class QuotaServiceImpl implements QuotaService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaServiceImpl.class);

    @Autowired
    private QuotaSyncConfig quotaSyncConfig;

    @Autowired
    private QuotaSyncService quotaSyncService;

    @Autowired
    private QuotaHelper quotaHelper;

    @Autowired
    private ScmConfClient confClient;

    @Autowired
    private ScmLockManager lockManger;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Autowired
    private QuotaSyncDao quotaSyncDao;

    @Autowired
    private QuotaConfigDao quotaConfigDao;

    @Autowired
    private QuotaSyncMsgSender quotaSyncMsgSender;

    @Override
    public QuotaResult enableQuota(String type, String name, long maxObjects, long maxSize,
            Long usedObjects, Long usedSize, Authentication auth) throws StatisticsException {
        logger.info("enable quota:type={},name={},maxSize={},maxObjects={}", type, name, maxSize,
                maxObjects);
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.ALL, "enable quota");
        boolean hasUsedQuotaParams = determineUsedParams(usedSize, usedObjects);
        if (maxObjects < 0) {
            maxObjects = -1;
        }
        if (maxSize < 0) {
            maxSize = -1;
        }
        ScmLock scmLock = null;
        try {
            scmLock = acquireQuotaManageLock(type, name);
            QuotaConfig quotaConfig = quotaHelper.getQuotaConfig(type, name);
            if (quotaConfig == null) {
                confClient.createConf(ScmConfigNameDefine.QUOTA, new QuotaConfig(type, name,
                        maxSize, maxObjects, true, 1, quotaHelper.generateExtraInfo(type, name)),
                        false);
            }
            else {
                if (!quotaConfig.isEnable()) {
                    QuotaUpdator quotaUpdator = new QuotaUpdator(type, name, maxSize, maxObjects,
                            true, new BasicBSONObject(FieldName.Quota.QUOTA_ROUND_NUMBER,
                                    quotaConfig.getQuotaRoundNumber()));
                    // 需要把上一次的额度信息置0
                    quotaUpdator.setUsedObjects(0L);
                    quotaUpdator.setUsedSize(0L);
                    quotaUpdator.setQuotaRoundNumber(quotaConfig.getQuotaRoundNumber() + 1);
                    confClient.updateConfig(ScmConfigNameDefine.QUOTA, quotaUpdator, false);
                }
                else {
                    throw new StatisticsException(StatisticsError.QUOTA_ALREADY_ENABLE,
                            type + " quota is already enabled:name=" + name);
                }
            }
            if (hasUsedQuotaParams) {
                logger.info(
                        "quota used info is exist, skip sync:type={},name={},usedSize={},usedObjects={}",
                        type, name, usedSize, usedObjects);
                return updateQuotaUsedInfo(type, name, usedObjects, usedSize, auth);
            }
            quotaSyncService.startSyncTask(type, name, true);
            return internalGetQuota(type, name);
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to enable " + type + " quota:name=" + name, e);
        }
        finally {
            if (scmLock != null) {
                scmLock.unlock();
            }
        }
    }

    private boolean determineUsedParams(Long usedSize, Long usedObjects)
            throws StatisticsException {
        if (usedSize == null && usedObjects == null) {
            return false;
        }
        if (usedSize == null || usedObjects == null) {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "usedSize and usedObjects must be both null or not null");
        }
        return true;
    }

    @Override
    public QuotaResult updateQuota(String type, String name, long maxObjects, long maxSize,
            Authentication auth) throws StatisticsException {
        logger.info("update quota:type={},name={},maxSize={},maxObjects={}", type, name, maxSize,
                maxObjects);
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.ALL, "update quota");
        if (maxObjects < 0) {
            maxObjects = -1;
        }
        if (maxSize < 0) {
            maxSize = -1;
        }
        ScmLock scmLock = null;
        try {
            scmLock = acquireQuotaManageLock(type, name);
            QuotaConfig quotaConfig = quotaHelper.getQuotaConfig(type, name);
            if (quotaConfig == null || !quotaConfig.isEnable()) {
                throw new StatisticsException(StatisticsError.QUOTA_NOT_ENABLE,
                        "failed to update quota," + type + " quota is not enable:name=" + name);
            }
            QuotaUpdator updator = new QuotaUpdator(type, name, maxSize, maxObjects, true,
                    new BasicBSONObject(FieldName.Quota.QUOTA_ROUND_NUMBER,
                            quotaConfig.getQuotaRoundNumber()));
            confClient.updateConfig(ScmConfigNameDefine.QUOTA, updator, false);
            return internalGetQuota(type, name);
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to update " + type + " quota:name=" + name, e);
        }
        finally {
            if (scmLock != null) {
                scmLock.unlock();
            }
        }
    }

    @Override
    public void disableQuota(String type, String name, Authentication auth)
            throws StatisticsException {
        logger.info("disable quota:type={},name={}", type, name);
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.ALL, "disable quota");
        ScmLock scmLock = null;
        try {
            scmLock = acquireQuotaManageLock(type, name);
            QuotaConfig quotaConfig = quotaHelper.getQuotaConfig(type, name);
            if (quotaConfig == null || !quotaConfig.isEnable()) {
                throw new StatisticsException(StatisticsError.QUOTA_ALREADY_DISABLE,
                        "failed to disable quota," + type + " quota is already disable:name="
                                + name);
            }
            QuotaUpdator quotaUpdator = new QuotaUpdator(type, name, -1L, -1L, false,
                    new BasicBSONObject(FieldName.Quota.QUOTA_ROUND_NUMBER,
                            quotaConfig.getQuotaRoundNumber()));
            confClient.updateConfig(ScmConfigNameDefine.QUOTA, quotaUpdator, false);
            QuotaSyncInfo quotaSyncInfo = quotaSyncDao.getQuotaSyncInfo(type, name, null);
            if (quotaSyncInfo != null
                    && ScmQuotaSyncStatus.SYNCING.getName().equals(quotaSyncInfo.getStatus())) {
                try {
                    quotaSyncDao.cancelSync(type, name);
                    quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name,
                            quotaSyncInfo.getSyncRoundNumber(),
                            quotaHelper.getS3AndContentServerInstance());
                }
                catch (Exception e) {
                    // 这里可以忽略更新同步表产生的异常，因为更新配置成功后，节点内部会变为无限额状态，同步表里面的记录不再使用
                    logger.error("failed to cancel sync:type={},name={}", type, name, e);
                }
            }
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to disable " + type + " quota:name=" + name, e);
        }
        finally {
            if (scmLock != null) {
                scmLock.unlock();
            }
        }

    }

    @Override
    public QuotaResult getQuota(String type, String name, Authentication auth)
            throws StatisticsException {
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.READ, "get quota");
        return internalGetQuota(type, name);
    }

    private QuotaResult internalGetQuota(String type, String name) throws StatisticsException {
        // 这里直接从限额表中查询，不走配置服务，因为配置服务返回的数据不包含已用额度信息
        QuotaConfigDetail quotaConfigDetail = quotaConfigDao.getQuotaConfigInfo(type, name);
        if (quotaConfigDetail == null) {
            QuotaResult result = new QuotaResult();
            result.setType(type);
            result.setName(name);
            return result;
        }
        QuotaSyncInfo statisticsInfo = null;
        if (quotaConfigDetail.isEnable()) {
            statisticsInfo = quotaSyncDao.getQuotaSyncInfo(type, name, null);
        }
        return buildQuotaResult(quotaConfigDetail, statisticsInfo);
    }

    @Override
    public QuotaResult updateQuotaUsedInfo(String type, String name, Long usedObjects,
            Long usedSize, Authentication auth) throws StatisticsException {
        logger.info("update quota used info:type={},name={},usedSize={},usedObjects={}", type, name,
                usedSize, usedObjects);
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.ALL, "update quota");
        if (usedSize == null && usedObjects == null) {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "used size and used objects can not be null at the same time");
        }
        if (usedObjects != null && usedObjects < 0) {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "used objects must be greater than or equal to 0");
        }
        if (usedSize != null && usedSize < 0) {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "used size must be greater than or equal to 0");
        }
        QuotaConfig quotaConfig = quotaHelper.getQuotaConfig(type, name);
        if (quotaConfig == null || !quotaConfig.isEnable()) {
            throw new StatisticsException(StatisticsError.QUOTA_NOT_ENABLE,
                    "failed to update quota," + type + " quota is not enable:name=" + name);
        }
        ScmLockPath lockPath = lockPathFactory.quotaUsedLockPath(type, name);
        ScmLock lock = null;
        try {
            lock = lockManger.acquiresLock(lockPath);
            quotaConfigDao.updateQuotaUsedInfo(type, name, usedObjects, usedSize);
        }
        catch (ScmLockException e) {
            throw new StatisticsException(StatisticsError.LOCK_ERROR, "failed to acquire lock", e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        return internalGetQuota(type, name);
    }

    private ScmLock acquireQuotaManageLock(String type, String name) throws StatisticsException {
        try {
            ScmLockPath lockPath = lockPathFactory.quotaManageLockPath(type, name);
            return lockManger.acquiresLock(lockPath);
        }
        catch (ScmLockException e) {
            throw new StatisticsException(StatisticsError.LOCK_ERROR,
                    "failed to acquire " + type + " quota lock:name=" + name, e);
        }
    }

    private QuotaResult buildQuotaResult(QuotaConfigDetail quotaConfig, QuotaSyncInfo syncInfo) {
        QuotaResult quotaResult = new QuotaResult();
        quotaResult.setType(quotaConfig.getType());
        quotaResult.setName(quotaConfig.getName());
        quotaResult.setMaxSize(quotaConfig.getMaxSize());
        quotaResult.setMaxObjects(quotaConfig.getMaxObjects());
        quotaResult.setUsedObjects(quotaConfig.getUsedObjects());
        quotaResult.setUsedSize(quotaConfig.getUsedSize());
        quotaResult.setEnable(quotaConfig.isEnable());
        quotaResult.setLastUpdateTime(quotaConfig.getLastUpdateTime());

        if (syncInfo != null) {
            quotaResult.setSyncStatus(syncInfo.getStatus());
            quotaResult.setErrorMsg(syncInfo.getErrorMsg());
            BSONObject progressDetail = syncInfo.getStatisticsDetail();
            if (progressDetail != null) {
                Number number = BsonUtils.getNumber(progressDetail,
                        FieldName.QuotaStatisticsProgress.ESTIMATED_TIME);
                if (number != null) {
                    quotaResult.setEstimatedEffectiveTime(number.longValue());
                }
            }
        }
        return quotaResult;
    }

}
