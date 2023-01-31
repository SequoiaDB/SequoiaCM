package com.sequoiacm.schedule.service;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfFeignClientFactory;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteUpdator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.schedule.bizconf.ScheduleStrategyMgr;
import com.sequoiacm.schedule.bizconf.ScmArgChecker;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.LifeCycleCommonTools;
import com.sequoiacm.schedule.common.LifeCycleConfigDefine;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.LifeCycleConfigFullEntity;
import com.sequoiacm.schedule.common.model.LifeCycleConfigUserEntity;
import com.sequoiacm.schedule.common.model.LifeCycleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleNewUserInfo;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.common.model.ScmCleanTriggers;
import com.sequoiacm.schedule.common.model.ScmExtraContent;
import com.sequoiacm.schedule.common.model.ScmFlow;
import com.sequoiacm.schedule.common.model.ScmTransitionTriggers;
import com.sequoiacm.schedule.common.model.TransitionEntityTranslator;
import com.sequoiacm.schedule.common.model.TransitionFullEntity;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.common.model.TransitionUserEntity;
import com.sequoiacm.schedule.core.LifeCycleCommonDefine;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.core.meta.SiteInfo;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.dao.LifeCycleConfigDao;
import com.sequoiacm.schedule.dao.LifeCycleScheduleDao;
import com.sequoiacm.schedule.dao.SiteDao;
import com.sequoiacm.schedule.dao.Transaction;
import com.sequoiacm.schedule.dao.TransactionFactory;
import com.sequoiacm.schedule.entity.ConfigEntityTranslator;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.entity.SiteEntity;
import com.sequoiacm.schedule.remote.ScheduleClient;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LifeCycleConfigServiceImpl implements LifeCycleConfigService {

    private static final Logger logger = LoggerFactory.getLogger(LifeCycleConfigServiceImpl.class);

    @Autowired
    private ScmLockManager scmLockManager;

    @Autowired
    private LifeCycleConfigDao lifeCycleConfigDao;

    @Autowired
    private LifeCycleScheduleDao lifeCycleScheduleDao;

    @Autowired
    private SiteDao siteDao;

    @Autowired
    private TransactionFactory transactionFactory;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScmConfFeignClientFactory configClientFactory;

    @Override
    public void setGlobalLifeCycleConfig(String user, LifeCycleConfigUserEntity info)
            throws ScheduleException {
        ScmLock lock = null;
        try {
            try {
                lock = lockGlobal();
            }
            catch (ScmLockException e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "set life cycle config failed,failed to lock global", e);
            }

            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                LifeCycleConfigFullEntity fullInfo = createConfigFullEntity(user, info);
                checkStageTagAndTransition(fullInfo);
                try {
                    lifeCycleConfigDao.insert(fullInfo);
                }
                catch (Exception e) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "set life cycle config failed", e);
                }
            }
            else {
                LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);

                // 检查是否是初始记录，即只有内置系统标签
                if (!isInitialConfig(fullInfo)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "set life cycle config failed, please delete old one first");
                }

                fullInfo = createConfigFullEntity(user, info);
                checkStageTagAndTransition(fullInfo);

                BSONObject insert = LifeCycleEntityTranslator.FullInfo.toBSONObject(fullInfo);
                Transaction t = transactionFactory.createTransaction();
                try {
                    t.begin();
                    lifeCycleConfigDao.delete(t);
                    lifeCycleConfigDao.insert(insert, t);
                    t.commit();
                }
                catch (Exception e) {
                    t.rollback();
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "set life cycle config failed", e);
                }
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public BSONObject getGlobalLifeCycleConfig() throws ScheduleException {
        try {
            return lifeCycleConfigDao.queryOne();
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "Can not get life cycle config", e);
        }
    }

    @Override
    public void deleteLifeCycleConfig() throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            BSONObject config = getGlobalLifeCycleConfig();
            if (config != null) {
                // 有站点或工作区使用，就不能删
                configUsedByWs();
                configUsedBySite();
                try {
                    lifeCycleConfigDao.delete();
                }
                catch (Exception e) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "Failed to delete life cycle config", e);
                }
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public void addGlobalStageTag(String user, String stageTagName, String stageTagDesc)
            throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                LifeCycleConfigUserEntity userEntity = LifeCycleEntityTranslator.UserInfo
                        .fromStageTagConfig(stageTagName, stageTagDesc);
                Set<String> stageTagNameSet = new HashSet<>();
                stageTagNameSet.add(stageTagName);
                addSystemStageTag(userEntity.getStageTagConfig(), stageTagNameSet);
                LifeCycleConfigFullEntity fullInfo = createConfigFullEntity(user, userEntity);
                lifeCycleConfigDao.insert(fullInfo);
            }
            else {
                LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);
                // check stage tag exist
                if (stageTagExist(fullInfo, stageTagName)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.REPEAT_STAGE_TAG_NAME,
                            "add stage tag failed,the stage tag already exist,stageTag="
                                    + stageTagName);
                }
                LifeCycleConfigFullEntity newFullInfo = LifeCycleEntityTranslator.FullInfo
                        .updateFullInfoByAddStageTag(fullInfo, stageTagName, stageTagDesc, user);
                BSONObject updator = new BasicBSONObject("$set",
                        LifeCycleEntityTranslator.FullInfo.toBSONObject(newFullInfo));
                lifeCycleConfigDao.update(new BasicBSONObject(), updator);
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public void removeGlobalStageTag(String stageTagName, String user) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            if (isSystemStageTag(stageTagName)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "system stage tag can not be remove ,stage tag name= " + stageTagName);
            }
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                        "remove stage tag failed, not set life cycle config");
            }
            else {
                LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);
                // check stage tag exist
                if (!stageTagExist(fullInfo, stageTagName)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.STAGE_TAG_NOT_EXIST,
                            "remove stage tag failed,the stage tag not found, stageTagConfig="
                                    + fullInfo.getStageTagConfig() + ", stageTag=" + stageTagName);
                }
                // check used by transition
                checkUsedByTransition(stageTagName, fullInfo.getTransitionConfig());

                if (stageTagUsedBySite(stageTagName)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_USED,
                            "remove stage tag failed, the stage tag already used by site");
                }
                LifeCycleConfigFullEntity newFullInfo = LifeCycleEntityTranslator.FullInfo
                        .updateFullInfoByRemoveStageTag(fullInfo, stageTagName, user);
                BSONObject updator = new BasicBSONObject("$set",
                        LifeCycleEntityTranslator.FullInfo.toBSONObject(newFullInfo));
                lifeCycleConfigDao.update(new BasicBSONObject(), updator);
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public BSONObject listGlobalStageTag() throws ScheduleException {
        BSONObject obj = getGlobalLifeCycleConfig();
        if (obj == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "get stage tag config failed, life cycle config not found");
        }
        else {
            LifeCycleConfigFullEntity info = LifeCycleEntityTranslator.FullInfo.fromBSONObject(obj);
            return info.getStageTagConfig();
        }
    }

    @Override
    public void addGlobalTransition(TransitionUserEntity info, String user) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                        "add global transition failed, life cycle config not found");
            }
            else {
                LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);
                TransitionFullEntity entity = getTransitionByName(fullInfo, info.getName());
                if (entity != null) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_EXIST,
                            "transition already exist, can not add again");
                }
                Set<String> stageTagNameSet = getStageTagSet(fullInfo);
                checkTransition(info, stageTagNameSet);
                LifeCycleConfigFullEntity newFullInfo = LifeCycleEntityTranslator.FullInfo
                        .updateFullInfoByAddTransition(fullInfo, info, user);
                BSONObject updator = new BasicBSONObject("$set",
                        LifeCycleEntityTranslator.FullInfo.toBSONObject(newFullInfo));
                lifeCycleConfigDao.update(new BasicBSONObject(), updator);
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public void removeGlobalTransition(String transitionName, String user) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                        "remove global transition failed, life cycle config not found");
            }
            else {
                LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);
                TransitionFullEntity entity = getTransitionByName(fullInfo, transitionName);
                if (entity != null) {
                    // check ws used this transition
                    if (transitionUsedByWs(entity.getId())) {
                        throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_USED_WS,
                                "remove global transition failed, transition used by workspaces");
                    }
                }
                else {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_NOT_EXIST,
                            "remove global transition failed, transition not found");
                }
                LifeCycleConfigFullEntity newFullInfo = LifeCycleEntityTranslator.FullInfo
                        .updateFullInfoByRemoveTransition(fullInfo, transitionName, user);
                BSONObject updator = new BasicBSONObject("$set",
                        LifeCycleEntityTranslator.FullInfo.toBSONObject(newFullInfo));
                lifeCycleConfigDao.update(new BasicBSONObject(), updator);
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public void updateGlobalTransition(String transitionName, TransitionUserEntity newInfo,
            String user) throws Exception {
        boolean checkAndCorrection = false;
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                        "update global transition failed, life cycle config not found");
            }

            LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(obj);
            TransitionFullEntity oldTransitionEntity = getTransitionByName(fullInfo,
                    transitionName);
            if (oldTransitionEntity == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_NOT_EXIST,
                        "update global transition failed, old transition not found, old transitionName"
                                + transitionName);
            }

            // check new transition valid
            Set<String> stageTagSet = getStageTagSet(fullInfo);
            checkTransition(newInfo, stageTagSet);

            String newName = newInfo.getName();
            if (!newName.equals(transitionName)) {
                // if alter name, check newName exist
                TransitionFullEntity existTransition = getTransitionByName(fullInfo, newName);
                if (existTransition != null) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "update global transition failed, new transition name already exist, new transitionName"
                                    + newName);
                }

                // 检查直接引用流的工作区是否已经存在新名字的流，有则不可以更改
                BasicBSONList cantAlterNameWs = new BasicBSONList();
                for (Object workspace : oldTransitionEntity.getWorkspaces()) {
                    if (existSameNameTransition((String) workspace, newName)) {
                        cantAlterNameWs.add(workspace);
                    }
                }
                if (cantAlterNameWs.size() > 0) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "update global transition failed,have workspace already exists new name transition, workspace="
                                    + cantAlterNameWs);
                }
            }

            LifeCycleConfigFullEntity newFullInfo = LifeCycleEntityTranslator.FullInfo
                    .updateFullInfoByAlterTransition(fullInfo, newInfo, oldTransitionEntity, user);
            BasicBSONList workspaces = oldTransitionEntity.getWorkspaces();

            String newSource = newInfo.getFlow().getSource();
            String newDest = newInfo.getFlow().getDest();
            if (oldTransitionEntity.getWorkspaces() != null
                    && !oldTransitionEntity.getWorkspaces().isEmpty()) {
                checkSiteModel(newSource, newDest);
            }
            // check update flow
            if (isAlterFlow(oldTransitionEntity, newSource, newDest)) {
                // 获取无法适配新流的工作区
                BasicBSONList wsByCantAlterFlow = getWsByCantAlterFlow(workspaces, newSource,
                        newDest);
                if (wsByCantAlterFlow.size() > 0) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "update global transition failed,have workspace can not have new flow stage tag site"
                                    + wsByCantAlterFlow);
                }
            }

            BSONObject updator = new BasicBSONObject("$set",
                    LifeCycleEntityTranslator.FullInfo.toBSONObject(newFullInfo));
            lifeCycleConfigDao.update(new BasicBSONObject(), updator);

            // get used transition ws schedule record
            List<TransitionScheduleEntity> updateEntities;
            try {
                updateEntities = listWsTransitionByGlobalId(oldTransitionEntity.getId());
            }
            catch (Exception e) {
                checkAndCorrection = true;
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "get workspace apply transition list failed,globalTransitionId="
                                + oldTransitionEntity.getId(),
                        e);
            }

            for (TransitionScheduleEntity wsOldEntity : updateEntities) {
                try {
                    updateWsTransitionByGlobal(wsOldEntity, newInfo, user);
                }
                catch (Exception e) {
                    checkAndCorrection = true;
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "workspace transition update error, workspace="
                                    + wsOldEntity.getWorkspace(),
                            e);
                }
            }
        }
        finally {
            unLock(lock);
            if (checkAndCorrection) {
                revote();
            }
        }
    }

    @Override
    public TransitionFullEntity getGlobalTransitionByName(String transitionName) throws Exception {
        BSONObject obj = getGlobalLifeCycleConfig();
        if (obj == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                    "get global transition failed, life cycle config not found");
        }
        else {
            LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(obj);
            TransitionFullEntity entity = getTransitionByName(fullInfo, transitionName);
            if (entity == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_NOT_EXIST,
                        "get global transition failed, transition not found, transitionName="
                                + transitionName);
            }
            return entity;
        }
    }

    private void updateWsTransitionFlowStageTag(String workspace, String oldStageTag,
            String newStageTag) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME,
                workspace);
        ScmBSONObjectCursor wsTransitionList = null;
        try {
            wsTransitionList = lifeCycleScheduleDao.query(matcher);
            List<TransitionScheduleEntity> updateFlowTransition = getNeedUpdateFlowTransition(
                    wsTransitionList, oldStageTag, newStageTag);
            Transaction t = transactionFactory.createTransaction();
            if (updateFlowTransition.size() > 0) {
                try {
                    t.begin();
                    for (TransitionScheduleEntity entity : updateFlowTransition) {
                        entity.setCustomized(true);
                        BSONObject updator = TransitionEntityTranslator.WsFullInfo
                                .toBSONObject(entity);
                        lifeCycleScheduleDao.update(updator, t);
                    }
                    t.commit();
                }
                catch (Exception e) {
                    t.rollback();
                    throw e;
                }
            }
        }
        finally {
            if (wsTransitionList != null) {
                wsTransitionList.close();
            }
        }

    }

    @Override
    public TransitionScheduleEntity wsApplyTransition(String user, String workspace,
            String transitionName, TransitionUserEntity customInfo, String preferredRegion,
            String preferredZone) throws Exception {
        boolean checkAndCorrection = false;
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                        "workspace apply transition failed, life cycle config not found");
            }

            LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(obj);

            TransitionFullEntity transitionFullEntity = getTransitionByName(fullInfo,
                    transitionName);

            if (transitionFullEntity == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "workspace apply transition failed,apply transition not exist,workspace="
                                + workspace + ", transition=" + transitionName);
            }

            // check ws had used this transition
            if (wsHadUsedTransition(workspace, transitionFullEntity)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "workspace apply transition failed,workspace already apply this transition,workspace="
                                + workspace);
            }

            TransitionScheduleEntity transitionSchedule;

            // had custom
            if (customInfo != null) {
                TransitionUserEntity overwriteTransition = overwriteTransition(transitionFullEntity,
                        customInfo);

                // check custom transition valid
                Set<String> stageTagSet = getStageTagSet(fullInfo);
                checkTransition(overwriteTransition, stageTagSet);

                transitionSchedule = createTransitionSchedule(overwriteTransition, user, workspace,
                        true, preferredRegion, preferredZone);
            }
            else {
                transitionSchedule = createTransitionSchedule(transitionFullEntity, user, workspace,
                        false, preferredRegion, preferredZone);
            }

            // 检查工作区是否存在同名的流
            if (existSameNameTransition(workspace, transitionSchedule.getTransition().getName())) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "a transition named already exists in the workspace,workspace=" + workspace
                                + ", transitionName="
                                + transitionSchedule.getTransition().getName());
            }

            // get source site id and dest site id from ws
            String sourceStageTag = transitionSchedule.getTransition().getFlow().getSource();
            String destStageTag = transitionSchedule.getTransition().getFlow().getDest();
            SiteInfo sourceSiteInfo = getSiteByStageTag(workspace, sourceStageTag);
            if (sourceSiteInfo == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                        "stage tag site not found form workspace, workspace=" + workspace
                                + " source stage tag=" + sourceStageTag);
            }
            SiteInfo destSiteInfo = getSiteByStageTag(workspace, destStageTag);
            if (destSiteInfo == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                        "stage tag site not found form workspace, workspace=" + workspace
                                + ", dest stage tag=" + destStageTag);
            }

            // check site
            WorkspaceInfo wsInfo = ScheduleServer.getInstance().getWorkspace(workspace);
            ScheduleStrategyMgr.getInstance().checkTransferSite(wsInfo, sourceSiteInfo.getId(),
                    destSiteInfo.getId());

            // create schedule
            BasicBSONList scheduleIds = createScheduleAndGetIds(transitionSchedule, sourceSiteInfo,
                    destSiteInfo, user, workspace);

            transitionSchedule.setScheduleIds(scheduleIds);
            transitionSchedule.setGlobalTransitionId(transitionFullEntity.getId());

            // insert config_schedule
            BSONObject insertInfo = TransitionEntityTranslator.WsFullInfo
                    .toBSONObject(transitionSchedule);

            Transaction t = transactionFactory.createTransaction();
            try {
                t.begin();
                lifeCycleScheduleDao.insert(insertInfo, t);
                if (!transitionSchedule.getCustomized()) {
                    // 直接引用的工作区，将工作区写入全局transition下的workspaces
                    // update global transition workspaces
                    BSONObject updatorLifeCycleConfig = updateTransitionAddWs(fullInfo,
                            transitionFullEntity, workspace);
                    lifeCycleConfigDao.update(updatorLifeCycleConfig, t);
                }
                t.commit();
            }
            catch (Exception e) {
                t.rollback();
                // remove already created schedule
                try {
                    removeSchedule(scheduleIds);
                }
                catch (Exception e1) {
                    logger.error("failed to delete schedule,scheduleIds={}", scheduleIds, e1);
                    checkAndCorrection = true;
                }
                throw e;
            }
            return transitionSchedule;
        }
        finally {
            unLock(lock);
            if (checkAndCorrection) {
                revote();
            }
        }
    }

    @Override
    public TransitionScheduleEntity wsUpdateTransition(String user, String workspace,
            String transitionName, TransitionUserEntity updateInfo, String preferredRegion,
            String preferredZone) throws Exception {
        boolean checkAndCorrection = false;
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            TransitionScheduleEntity oldTransitionSchedule = findWsTransition(workspace,
                    transitionName);
            if (oldTransitionSchedule == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                        "old transition not found,transition name=" + transitionName
                                + " workspace name=" + workspace);
            }

            BSONObject obj = getGlobalLifeCycleConfig();
            LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(obj);

            if (updateInfo.getName() != null && !updateInfo.getName().equals(transitionName)) {
                // 如果工作区改流名字，检查新名字是否已经存在
                if (existSameNameTransition(workspace, updateInfo.getName())) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "can not alter transition name, the new transition name already exists, workspace="
                                    + workspace + ", transitionName=" + updateInfo.getName());
                }
            }

            if (isOnlyAlterName(updateInfo)) {
                TransitionUserEntity newTransition = oldTransitionSchedule.getTransition();
                newTransition.setName(updateInfo.getName());
                TransitionScheduleEntity updator = updateTransitionSchedule(oldTransitionSchedule,
                        newTransition, oldTransitionSchedule.getPreferredRegion(),
                        oldTransitionSchedule.getPreferredZone(), user);
                updator.setGlobalTransitionId(oldTransitionSchedule.getGlobalTransitionId());
                BSONObject updatorBson = TransitionEntityTranslator.WsFullInfo
                        .toBSONObject(updator);

                Transaction t = transactionFactory.createTransaction();
                try {
                    t.begin();
                    lifeCycleScheduleDao.update(updatorBson, t);
                    if (!oldTransitionSchedule.getCustomized()) {
                        // 不是自定义变为自定义，从全局transition的workspaces 剔除该工作区
                        TransitionFullEntity transition = getTransitionById(fullInfo,
                                oldTransitionSchedule.getGlobalTransitionId());
                        if (null != transition) {
                            BSONObject object = updateTransitionRemoveWs(fullInfo, transition,
                                    workspace);
                            lifeCycleConfigDao.update(object, t);
                        }
                        else {
                            logger.warn(
                                    "Can ignore error!Failed to update transition workspaces, life cycle config not found transition");
                        }
                    }
                    t.commit();
                }
                catch (Exception e) {
                    t.rollback();
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "update transition name failed, newName=" + updateInfo.getName(), e);
                }
                return updator;
            }
            else {
                BasicBSONList oldScheduleIds = oldTransitionSchedule.getScheduleIds();
                Set<String> stageTagSet = getStageTagSet(fullInfo);

                // check new transition valid
                checkTransition(updateInfo, stageTagSet);

                // 如果没有region和zone，则使用旧的region和zone
                if (null == preferredRegion) {
                    preferredRegion = oldTransitionSchedule.getPreferredRegion();
                }
                if (null == preferredZone) {
                    preferredZone = oldTransitionSchedule.getPreferredZone();
                }
                TransitionScheduleEntity updateTransitionSchedule = updateTransitionSchedule(
                        oldTransitionSchedule, updateInfo, preferredRegion, preferredZone, user);

                // get source site id and dest site id from ws
                String sourceStageTag = updateTransitionSchedule.getTransition().getFlow()
                        .getSource();
                String destStageTag = updateTransitionSchedule.getTransition().getFlow().getDest();
                SiteInfo sourceSiteInfo = getSiteByStageTag(workspace, sourceStageTag);
                if (sourceSiteInfo == null) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                            "stage tag site not found form workspace, workspace=" + workspace
                                    + " source stage tag=" + sourceStageTag);
                }
                SiteInfo destSiteInfo = getSiteByStageTag(workspace, destStageTag);
                if (destSiteInfo == null) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                            "stage tag site not found form workspace, workspace=" + workspace
                                    + " dest stage tag=" + destStageTag);
                }

                // check site
                WorkspaceInfo wsInfo = ScheduleServer.getInstance().getWorkspace(workspace);
                ScheduleStrategyMgr.getInstance().checkTransferSite(wsInfo, sourceSiteInfo.getId(),
                        destSiteInfo.getId());

                // 先停掉旧的调度任务，再创建新的
                if (oldTransitionSchedule.isEnable()) {
                    // 如果调度任务本来就是关闭的，则不用停
                    alterScheduleSilence(oldScheduleIds, false);
                }

                BasicBSONList newScheduleIds = createScheduleAndGetIds(updateTransitionSchedule,
                        sourceSiteInfo, destSiteInfo, user, workspace);
                updateTransitionSchedule.setScheduleIds(newScheduleIds);
                updateTransitionSchedule
                        .setGlobalTransitionId(oldTransitionSchedule.getGlobalTransitionId());
                BSONObject updatorInfo = TransitionEntityTranslator.WsFullInfo
                        .toBSONObject(updateTransitionSchedule);

                Transaction t = transactionFactory.createTransaction();
                try {
                    t.begin();
                    lifeCycleScheduleDao.update(updatorInfo, t);
                    if (!oldTransitionSchedule.getCustomized()) {
                        // 不是自定义变为自定义，从全局transition的workspaces 剔除该工作区
                        TransitionFullEntity transition = getTransitionById(fullInfo,
                                oldTransitionSchedule.getGlobalTransitionId());
                        if (null != transition) {
                            BSONObject object = updateTransitionRemoveWs(fullInfo, transition,
                                    workspace);
                            lifeCycleConfigDao.update(object, t);
                        }
                        else {
                            logger.warn(
                                    "Can ignore error!Failed to update transition workspaces, life cycle config not found transition");
                        }
                    }
                    t.commit();
                }
                catch (Exception e) {
                    t.rollback();
                    // 删掉新创建的调度任务
                    try {
                        removeSchedule(newScheduleIds);
                    }
                    catch (Exception e1) {
                        logger.error("failed to delete schedule,scheduleIds={}", newScheduleIds,
                                e1);
                        checkAndCorrection = true;
                    }
                    if (oldTransitionSchedule.isEnable()) {
                        alterScheduleSilence(oldScheduleIds, true);
                    }
                    throw e;
                }
                try {
                    removeSchedule(oldScheduleIds);
                }
                catch (Exception e) {
                    logger.error("failed to delete schedule,scheduleIds={}", oldScheduleIds, e);
                }
                return updateTransitionSchedule;
            }
        }
        finally {
            unLock(lock);
            if (checkAndCorrection) {
                revote();
            }
        }
    }

    @Override
    public void wsRemoveTransition(String workspace, String transitionName) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            TransitionScheduleEntity entity = lifeCycleScheduleDao.queryByName(workspace,
                    transitionName);
            if (entity == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_NOT_EXIST,
                        "transition not found,transition name=" + transitionName
                                + " workspace name=" + workspace);
            }

            // update global transition workspaces
            BSONObject obj = getGlobalLifeCycleConfig();
            if (obj == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                        "workspace remove transition failed, life cycle config not found");
            }

            LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(obj);
            BSONObject updatorLifeCycleConfig = null;
            TransitionFullEntity transitionFullEntity = getTransitionById(fullInfo,
                    entity.getGlobalTransitionId());
            if (transitionFullEntity == null) {
                logger.warn(
                        "Can ignore error!Failed to update transition workspaces, life cycle config not found transition, transitionName={}",
                        transitionName);
            }
            else {
                if (!entity.getCustomized()) {
                    // 非自定义需要移除全局transition中workspaces中的workspace
                    updatorLifeCycleConfig = updateTransitionRemoveWs(fullInfo,
                            transitionFullEntity, workspace);
                }
            }

            BasicBSONList scheduleIds = entity.getScheduleIds();
            // 先停掉关联的调度任务
            if (entity.isEnable()) {
                alterScheduleSilence(scheduleIds, false);
            }

            Transaction t = transactionFactory.createTransaction();
            try {
                t.begin();
                lifeCycleScheduleDao.delete(entity.getId(), t);
                if (updatorLifeCycleConfig != null) {
                    lifeCycleConfigDao.update(updatorLifeCycleConfig, t);
                }
                t.commit();
            }
            catch (Exception e) {
                t.rollback();
                // 删除记录失败，将关联的调度任务重启
                if (entity.isEnable()) {
                    alterScheduleSilence(scheduleIds, true);
                }
                throw e;
            }
            try {
                // 删除掉关联的调度任务
                removeSchedule(scheduleIds);
            }
            catch (Exception e) {
                logger.error("failed to delete schedule,scheduleIds={}", scheduleIds, e);
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public TransitionScheduleEntity findWsTransition(String workspace, String transitionName)
            throws Exception {
        return lifeCycleScheduleDao.queryByName(workspace, transitionName);
    }

    @Override
    public List<TransitionScheduleEntity> listWsTransition(String workspace) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME,
                workspace);
        ScmBSONObjectCursor cursor = null;
        try {
            cursor = lifeCycleScheduleDao.query(matcher);
            List<TransitionScheduleEntity> result = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject obj = cursor.next();
                TransitionScheduleEntity entity = TransitionEntityTranslator.WsFullInfo
                        .fromBSONObject(obj);
                result.add(entity);
            }
            return result;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public List<TransitionUserEntity> listGlobalTransitionByStageTag(String stageTag)
            throws Exception {
        BSONObject obj = getGlobalLifeCycleConfig();
        if (obj == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                    "life cycle config not found");
        }

        LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo.fromBSONObject(obj);
        if (!stageTagExist(fullInfo, stageTag)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.STAGE_TAG_NOT_EXIST,
                    "stage tag not exist, stage tag=" + stageTag);
        }

        return getGlobalTransitionsUsedStageTag(fullInfo.getTransitionConfig(), stageTag);
    }

    @Override
    public BSONObject listWsApplyTransition(String transitionName)
            throws Exception {
        BSONObject result = new BasicBSONObject();
        BSONObject globalLifeCycleConfig = getGlobalLifeCycleConfig();
        if (globalLifeCycleConfig == null) {
            return result;
        }
        else {
            LifeCycleConfigFullEntity fullEntity = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(globalLifeCycleConfig);
            TransitionFullEntity full = getTransitionByName(fullEntity, transitionName);
            if (full == null) {
                return result;
            }
            else {
                List<TransitionScheduleEntity> list = listWsTransitionByGlobalId(full.getId());
                BasicBSONList customizedList = new BasicBSONList();
                BasicBSONList uncustomizedList = new BasicBSONList();
                for (TransitionScheduleEntity entity : list) {
                    String workspace = entity.getWorkspace();
                    if (entity.getCustomized()) {
                        customizedList.add(workspace);
                    }
                    else {
                        uncustomizedList.add(workspace);
                    }
                }
                result.put("customized", customizedList);
                result.put("uncustomized", uncustomizedList);
            }
        }
        return result;
    }

    @Override
    public List<TransitionUserEntity> listGlobalTransition() throws Exception {
        BSONObject obj = getGlobalLifeCycleConfig();
        if (obj == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_NOT_EXIST,
                    "life cycle config not found");
        }

        LifeCycleConfigFullEntity fullInfo = LifeCycleEntityTranslator.FullInfo.fromBSONObject(obj);

        List<TransitionUserEntity> result = new ArrayList<>();
        BasicBSONList transitionConfig = fullInfo.getTransitionConfig();
        for (Object o : transitionConfig) {
            BSONObject transition = (BSONObject) o;
            result.add(TransitionEntityTranslator.FullInfo.fromBSONObject(transition));
        }
        return result;
    }

    @Override
    public void wsUpdateTransitionStatus(String user, String workspace, String transitionName,
            boolean enable) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            TransitionScheduleEntity entity = findWsTransition(workspace, transitionName);
            if (null == entity) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.TRANSITION_NOT_EXIST,
                        "transition not found, workspace=" + workspace + ", transitionName="
                                + transitionName);
            }
            if (enable != entity.isEnable()) {
                entity.setEnable(enable);
                entity.setUpdateTime(new Date().getTime());
                entity.setUpdateUser(user);
                BasicBSONList scheduleIds = entity.getScheduleIds();
                Transaction t = transactionFactory.createTransaction();
                try {
                    t.begin();
                    lifeCycleScheduleDao
                            .update(TransitionEntityTranslator.WsFullInfo.toBSONObject(entity), t);
                    alterScheduleSilence(scheduleIds, enable);
                    t.commit();
                }
                catch (Exception e) {
                    t.rollback();
                    throw e;
                }
            }
        }
        finally {
            if (null != lock) {
                unLock(lock);
            }
        }
    }

    /**
     * start once transition is start move task or copy task options:{ "filter":
     * condition, "scope": 3, "max_exec_time": 7000, "quick_start": false,
     * "recycle_space": true, "data_check_level": strict }
     */
    @Override
    public BSONObject startOnceTransition(String workspaceName, String options,
            String sourceStageTag, String destStageTag, String userDetail, String sessionId,
            String preferredRegion, String preferredZone, int type) throws Exception {
        SiteInfo sourceSiteInfo = getSiteByStageTag(workspaceName, sourceStageTag);
        if (sourceSiteInfo == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "stage tag site not found form workspace, workspace=" + workspaceName
                            + " source stage tag=" + sourceStageTag);
        }

        SiteInfo destSiteInfo = getSiteByStageTag(workspaceName, destStageTag);
        if (destSiteInfo == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "stage tag site not found form workspace, workspace=" + workspaceName
                            + " dest stage tag=" + destStageTag);
        }
        FileServerEntity server = ScheduleServer.getInstance()
                .getRandomServer(sourceSiteInfo.getId(), preferredRegion, preferredZone);
        if (server != null) {
            ScheduleClient contentServerClient = createContentServerClient(server);
            BSONObject res = contentServerClient.createMoveCopyTask(sessionId, userDetail,
                    workspaceName, type, destSiteInfo.getName(), options);
            return res;
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not found content server, preferredRegion=" + preferredRegion
                            + ", preferredZone=" + preferredZone + ", siteId="
                            + sourceSiteInfo.getId());
        }
    }

    @Override
    public void setSiteStageTag(String siteName, String stageTag) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            SiteInfo siteInfo = ScheduleServer.getInstance().getSite(siteName);
            if (null == siteInfo) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "site not exist:siteName=" + siteName);
            }
            String siteStageTag = siteInfo.getStageTag();
            if (StringUtils.hasText(siteStageTag)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "site already had stage tag,stage_tag=" + siteStageTag);
            }
            else {
                BSONObject obj = getGlobalLifeCycleConfig();
                if (obj == null) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "failed to set site stage tag, life cycle config not exist");
                }
                LifeCycleConfigFullEntity lifeCycleConfig = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);
                if (!stageTagExist(lifeCycleConfig, stageTag)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "the stage tag not found from life cycle config,stage_tag=" + stageTag);
                }
                checkOtherSiteUsedTag(siteName, stageTag);
                updateSiteConf(siteName, stageTag);
            }
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public void alterSiteStageTag(String siteName, String newStageTag) throws Exception {
        boolean checkAndCorrection = false;
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            SiteInfo siteInfo = ScheduleServer.getInstance().getSite(siteName);
            if (null == siteInfo) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "site not exist:siteName=" + siteName);
            }
            String siteStageTag = siteInfo.getStageTag();
            if (StringUtils.hasText(siteStageTag)) {
                if (siteStageTag.equals(newStageTag)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "the new stage tag should be different with the old stage tag, siteName="
                                    + siteName + ", oldStageTag=" + siteStageTag + ", newStageTag="
                                    + newStageTag);
                }
                checkOtherSiteUsedTag(siteName, newStageTag);

                BSONObject obj = getGlobalLifeCycleConfig();
                if (obj == null) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "failed to alter site stage tag, life cycle config not exist");
                }
                LifeCycleConfigFullEntity lifeCycleConfig = LifeCycleEntityTranslator.FullInfo
                        .fromBSONObject(obj);
                if (!stageTagExist(lifeCycleConfig, newStageTag)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "the new stage tag not found from life cycle config,stageTag="
                                    + newStageTag);
                }

                List<WorkspaceInfo> workspaceInfoList = getWorkspaceList(siteName);
                for (WorkspaceInfo scmWorkspaceInfo : workspaceInfoList) {
                    // 更新工作区下对应流的Transition的flow
                    try {
                        updateWsTransitionFlowStageTag(scmWorkspaceInfo.getName(), siteStageTag,
                                newStageTag);
                        logger.info(
                                "update workspace transition flow by update site stage tag, workspace="
                                        + scmWorkspaceInfo.getName() + ", oldStageTag="
                                        + siteStageTag + ", newStageTag=" + newStageTag);
                    }
                    catch (Exception e) {
                        checkAndCorrection = true;
                        throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                                "failed to update ws transition flow,can not update site ,site="
                                        + siteName + ", stageTag=" + siteStageTag,
                                e);
                    }
                }
                try {
                    updateSiteConf(siteName, newStageTag);
                }
                catch (Exception e) {
                    checkAndCorrection = true;
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "can not update site stage tag,site=" + siteName + ", stageTag="
                                    + siteStageTag,
                            e);
                }
            }
            else {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "the site stage tag is null, don't update,site=" + siteName);
            }
        }
        finally {
            unLock(lock);
            if (checkAndCorrection) {
                revote();
            }
        }
    }

    @Override
    public void deleteSiteStageTag(String siteName) throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            SiteInfo siteInfo = ScheduleServer.getInstance().getSite(siteName);
            if (null == siteInfo) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "site not exist:siteName=" + siteName);
            }
            String siteStageTag = siteInfo.getStageTag();
            if (StringUtils.hasText(siteStageTag)) {
                List<WorkspaceInfo> workspaceInfoList = getWorkspaceList(siteName);
                // 检查工作区的流是否使用到了该阶段标签
                for (WorkspaceInfo scmWorkspaceInfo : workspaceInfoList) {
                    checkWsTransitionUsed(scmWorkspaceInfo.getName(), siteStageTag);
                }
            }
            updateSiteConf(siteName, "");
        }
        finally {
            unLock(lock);
        }
    }

    @Override
    public String getSiteStageTag(String siteName) throws ScheduleException {
        SiteInfo site = ScheduleServer.getInstance().getSite(siteName);
        if (site == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "site not exist:siteName=" + siteName);
        }
        return site.getStageTag();
    }

    private boolean existSameNameTransition(String workspace, String transitionName)
            throws Exception {
        TransitionScheduleEntity entity = lifeCycleScheduleDao.queryByName(workspace,
                transitionName);
        return entity != null;
    }

    private void revote() {
        try {
            ScheduleElector.getInstance().quitAndReVote();
        }
        catch (Exception e) {
            logger.error("revote failed", e);
            ScheduleCommonTools.exitProcess();
        }
    }

    private void checkWsTransitionUsed(String workspace, String stageTag) throws Exception {
        List<TransitionScheduleEntity> transitionScheduleList = listWsTransition(workspace);
        for (TransitionScheduleEntity entity : transitionScheduleList) {
            TransitionUserEntity transition = entity.getTransition();
            if (transition != null) {
                String source = transition.getFlow().getSource();
                String dest = transition.getFlow().getDest();
                if (stageTag.equals(source) || stageTag.equals(dest)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "the stage tag already used by transition,workspace=" + workspace
                                    + " ,transitionName=" + transition.getName());
                }
            }
        }
    }

    private void updateSiteConf(String siteName, String stageTag) throws ScheduleException {
        try {
            SiteUpdator siteUpdator = new SiteUpdator(siteName, stageTag);
            configClientFactory.getClient().updateConf(ScmConfigNameDefine.SITE,
                    siteUpdator.toBSONObject(), false);
        }
        catch (ScmConfigException e) {
            if (e.getError() == ScmConfError.SITE_NOT_EXIST) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "site not exist, siteName=" + siteName, e);
            }
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to update site: site=" + siteName + "， stageTag=" + stageTag, e);
        }
    }

    // 站点在同工作区下，不可以有相同的阶段标签
    private void checkOtherSiteUsedTag(String siteName, String stageTag) throws ScheduleException {
        List<WorkspaceInfo> workspaceList = getWorkspaceList(siteName);
        // check stage tag used by workspace's site
        for (WorkspaceInfo workspaceInfo : workspaceList) {
            SiteInfo usedStageTagSite = getSiteByStageTag(workspaceInfo.getName(), stageTag);
            if (usedStageTagSite != null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "the stage tag already used by site,siteName=" + usedStageTagSite.getName()
                                + " ,workspaceName=" + workspaceInfo.getName());
            }
        }
    }

    private List<WorkspaceInfo> getWorkspaceList(String siteName) {
        List<WorkspaceInfo> haveSiteWsList = new ArrayList<>();
        List<WorkspaceInfo> allWorkspace = ScheduleServer.getInstance().getAllWorkspace();
        for (WorkspaceInfo workspaceInfo : allWorkspace) {
            SiteEntity site = workspaceInfo.getSite(siteName);
            if (site != null) {
                haveSiteWsList.add(workspaceInfo);
            }
        }
        return haveSiteWsList;
    }

    private void checkSiteModel(String source, String dest) throws ScheduleException {
        List<SiteInfo> sourceSiteList = querySite(source);
        List<SiteInfo> destSiteList = querySite(dest);
        if (sourceSiteList.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not find site, stageTag=" + source);
        }
        if (destSiteList.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not find site, stageTag=" + dest);
        }
        StrategyType strategyType = ScheduleStrategyMgr.getInstance().strategyType();
        if (strategyType == StrategyType.STAR) {
            SiteInfo rootSite = ScheduleServer.getInstance().getRootSite();
            int mainSiteId = rootSite.getId();
            for (SiteInfo sourceSite : sourceSiteList) {
                for (SiteInfo destSite : destSiteList) {
                    int sourceSiteId = sourceSite.getId();
                    int destSiteId = destSite.getId();
                    if (sourceSiteId == destSiteId) {
                        throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                                "The source site and dest site cannot be the same" + ":sourceSite="
                                        + sourceSiteId + ",destSiteId=" + destSiteId);
                    }
                    if (sourceSiteId != mainSiteId && destSiteId != mainSiteId) {
                        throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                                "Under the star strategy, cannot transfer file from branch site to branch site"
                                        + ":sourceSite=" + sourceSiteId + ",destSiteId="
                                        + destSiteId);
                    }
                }
            }
        }
    }

    private List<SiteInfo> querySite(String stageTag) throws ScheduleException {
        List<SiteInfo> result = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject(FieldName.Site.FIELD_STAGE_TAG, stageTag);
        ScmBSONObjectCursor siteCursor = null;
        try {
            siteCursor = siteDao.query(matcher);
            while (siteCursor.hasNext()) {
                BSONObject siteInfo = siteCursor.next();
                int siteId = BsonUtils.getIntegerChecked(siteInfo, FieldName.Site.FIELD_ID);
                result.add(ScheduleServer.getInstance().getSite(siteId));
            }
            return result;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not find site, stageTag=" + stageTag, e);
        }
        finally {
            if (null != siteCursor) {
                siteCursor.close();
            }
        }
    }

    private boolean isOnlyAlterName(TransitionUserEntity updateInfo) {
        if (updateInfo.getName() == null) {
            return false;
        }
        else {
            return updateInfo.getFlow() == null && updateInfo.getExtraContent() == null
                    && updateInfo.getTransitionTriggers() == null
                    && updateInfo.getCleanTriggers() == null && updateInfo.getMatcher() == null;
        }
    }

    private boolean transitionUsedByWs(String transitionId) throws ScheduleException {
        BSONObject matcher = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID,
                transitionId);
        ScmBSONObjectCursor query = null;
        try {
            query = lifeCycleScheduleDao.query(matcher);
            return query.hasNext();
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to check transition used by workspace", e);
        }
        finally {
            if (query != null) {
                query.close();
            }
        }
    }

    private boolean isInitialConfig(LifeCycleConfigFullEntity fullInfo) {
        BasicBSONList transitionConfig = fullInfo.getTransitionConfig();
        if (transitionConfig != null && transitionConfig.size() > 0) {
            return false;
        }

        BasicBSONList stageTagConfig = fullInfo.getStageTagConfig();
        for (Object o : stageTagConfig) {
            BSONObject stageTag = (BSONObject) o;
            String name = (String) stageTag.get(FieldName.LifeCycleConfig.FIELD_STAGE_NAME);
            if (!isSystemStageTag(name)) {
                return false;
            }
        }
        return true;
    }

    private ScheduleClient createContentServerClient(FileServerEntity server) {
        String targetUrl = ScheduleCommonTools.createContentServerUrl(server.getHostName(),
                server.getPort());
        ScheduleClientFactory clientFactory = ScheduleMgrWrapper.getInstance().getClientFactory();
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(targetUrl);

        return client;
    }

    private void alterScheduleSilence(BasicBSONList scheduleIds, boolean enableFlag) {
        for (Object obj : scheduleIds) {
            String scheduleId = (String) obj;
            ScheduleNewUserInfo newScheduleInfo = new ScheduleNewUserInfo();
            newScheduleInfo.setEnable(enableFlag);
            try {
                scheduleService.updateSchedule(scheduleId, newScheduleInfo);
            }
            catch (Exception e) {
                logger.error("transition schedule alter enable failed, scheduleId=" + scheduleId
                        + ", enable=" + enableFlag, e);
            }
        }
    }

    private void updateWsTransitionByGlobal(TransitionScheduleEntity wsOldInfo,
            TransitionUserEntity newInfo, String user) throws Exception {
        if (wsOldInfo.getCustomized()) {
            return;
        }
        String workspace = wsOldInfo.getWorkspace();
        BasicBSONList oldScheduleIds = wsOldInfo.getScheduleIds();
        TransitionScheduleEntity newEntity = updateTransitionScheduleByGlobal(wsOldInfo, newInfo,
                wsOldInfo.getPreferredRegion(), wsOldInfo.getPreferredZone(), user);

        // get source site id and dest site id from ws
        String sourceStageTag = newEntity.getTransition().getFlow().getSource();
        String destStageTag = newEntity.getTransition().getFlow().getDest();
        SiteInfo sourceSiteInfo = getSiteByStageTag(workspace, sourceStageTag);
        if (sourceSiteInfo == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "stage tag site not found form workspace, workspace=" + workspace
                            + " source stage tag=" + sourceStageTag);
        }
        SiteInfo destSiteInfo = getSiteByStageTag(workspace, destStageTag);
        if (destSiteInfo == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.SITE_NOT_EXISTS,
                    "stage tag site not found form workspace, workspace=" + workspace
                            + " dest stage tag=" + destStageTag);
        }

        // check site
        WorkspaceInfo wsInfo = ScheduleServer.getInstance().getWorkspace(workspace);
        ScheduleStrategyMgr.getInstance().checkTransferSite(wsInfo, sourceSiteInfo.getId(),
                destSiteInfo.getId());

        // 先停掉旧的
        if (wsOldInfo.isEnable()) {
            alterScheduleSilence(oldScheduleIds, false);
        }
        BasicBSONList newScheduleIds = createScheduleAndGetIds(newEntity, sourceSiteInfo,
                destSiteInfo, user, workspace);
        newEntity.setScheduleIds(newScheduleIds);
        newEntity.setGlobalTransitionId(wsOldInfo.getGlobalTransitionId());
        BSONObject updatorInfo = TransitionEntityTranslator.WsFullInfo.toBSONObject(newEntity);
        try {
            lifeCycleScheduleDao.update(updatorInfo, null);
        }
        catch (Exception e) {
            try {
                removeSchedule(newScheduleIds);
            }
            catch (Exception e1) {
                logger.error("failed to delete schedule,scheduleIds={}", newScheduleIds, e1);
            }

            if (wsOldInfo.isEnable()) {
                alterScheduleSilence(oldScheduleIds, true);
            }
            throw e;
        }
        try {
            removeSchedule(oldScheduleIds);
        }
        catch (Exception e) {
            logger.error("failed to delete schedule,scheduleIds={}", newScheduleIds, e);
        }

    }

    private List<TransitionScheduleEntity> listWsTransitionByGlobalId(String id) throws Exception {
        List<TransitionScheduleEntity> resultList = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID,
                id);
        ScmBSONObjectCursor cursor = null;
        try {
            cursor = lifeCycleScheduleDao.query(matcher);
            while (cursor.hasNext()) {
                BSONObject obj = cursor.next();
                TransitionScheduleEntity entity = TransitionEntityTranslator.WsFullInfo
                        .fromBSONObject(obj);
                resultList.add(entity);
            }
            return resultList;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private BasicBSONList getWsByCantAlterFlow(BasicBSONList workspaces, String newSource,
            String newDest) throws ScheduleException {
        BasicBSONList cantAlterFlowWs = new BasicBSONList();
        for (Object obj : workspaces) {
            String workspace = (String) obj;
            SiteInfo sourceSite = getSiteByStageTag(workspace, newSource);
            SiteInfo destSite = getSiteByStageTag(workspace, newDest);
            if (sourceSite == null || destSite == null) {
                cantAlterFlowWs.add(workspace);
            }
        }
        return cantAlterFlowWs;
    }

    private boolean isAlterFlow(TransitionUserEntity oldInfo, String newSource, String newDest) {
        String oldSource = oldInfo.getFlow().getSource();
        String oldDest = oldInfo.getFlow().getDest();
        return !oldSource.equals(newSource) || !oldDest.equals(newDest);
    }

    private BasicBSONList createScheduleAndGetIds(TransitionScheduleEntity transitionSchedule,
            SiteInfo sourceSiteInfo, SiteInfo destSiteInfo, String user, String workspace)
            throws Exception {
        Date date = new Date();
        BasicBSONList scheduleIds = new BasicBSONList();
        if (transitionSchedule.getTransition().getCleanTriggers() == null) {
            // create move_file schedule
            ScheduleUserEntity moveFileSchedule = LifeCycleCommonTools.createScheduleUserEntity(
                    ScheduleDefine.ScheduleType.MOVE_FILE, transitionSchedule,
                    sourceSiteInfo.getName(), destSiteInfo.getName(),
                    transitionSchedule.getPreferredRegion(), transitionSchedule.getPreferredZone(),
                    date);

            try {
                ScheduleFullEntity moveFileScheduleFullInfo = scheduleService
                        .createSchedule(user, moveFileSchedule);
                scheduleIds.add(moveFileScheduleFullInfo.getId());
            }
            catch (Exception e) {
                try {
                    removeSchedule(scheduleIds);
                }
                catch (Exception e1) {
                    logger.error("failed to delete schedule,scheduleIds={}", scheduleIds, e1);
                }
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to apply transition,can not create move file schedule,workspace="
                                + workspace + " ,transition=" + transitionSchedule.getTransition(),
                        e);
            }

        }
        else {
            // create copy and clean schedule
            ScheduleUserEntity copyFileSchedule = LifeCycleCommonTools.createScheduleUserEntity(
                    ScheduleDefine.ScheduleType.COPY_FILE, transitionSchedule,
                    sourceSiteInfo.getName(), destSiteInfo.getName(),
                    transitionSchedule.getPreferredRegion(),
                    transitionSchedule.getPreferredZone(), date);
            try {
                ScheduleFullEntity copyFileScheduleFullInfo = scheduleService
                        .createSchedule(user, copyFileSchedule);
                scheduleIds.add(copyFileScheduleFullInfo.getId());
            }
            catch (Exception e) {
                try {
                    removeSchedule(scheduleIds);
                }
                catch (Exception e1) {
                    logger.error("failed to delete to schedule,scheduleIds={}", scheduleIds);
                }
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to apply transition,can not create copy file schedule,workspace="
                                + workspace + " ,transition=" + transitionSchedule.getTransition(),
                        e);
            }

            ScheduleUserEntity cleanFileSchedule = LifeCycleCommonTools.createScheduleUserEntity(
                    ScheduleDefine.ScheduleType.CLEAN_FILE, transitionSchedule,
                    sourceSiteInfo.getName(), destSiteInfo.getName(),
                    transitionSchedule.getPreferredRegion(),
                    transitionSchedule.getPreferredZone(), date);

            try {
                ScheduleFullEntity cleanFileScheduleFullInfo = scheduleService
                        .createSchedule(user, cleanFileSchedule);
                scheduleIds.add(cleanFileScheduleFullInfo.getId());
            }
            catch (Exception e) {
                try {
                    removeSchedule(scheduleIds);
                }
                catch (Exception e1) {
                    logger.error("failed to delete schedule,scheduleIds={}", scheduleIds, e1);
                }

                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to apply transition,can not create clean file schedule,workspace="
                                + workspace + " ,transition=" + transitionSchedule.getTransition(),
                        e);
            }
        }
        return scheduleIds;
    }

    private List<TransitionUserEntity> getGlobalTransitionsUsedStageTag(
            BasicBSONList transitionConfig, String stageTag) {
        List<TransitionUserEntity> result = new ArrayList<>();
        for (Object o : transitionConfig) {
            BSONObject obj = (BSONObject) o;
            TransitionFullEntity fullInfo = TransitionEntityTranslator.FullInfo.fromBSONObject(obj);
            String source = fullInfo.getFlow().getSource();
            String dest = fullInfo.getFlow().getDest();
            if (stageTag.equals(source) || stageTag.equals(dest)) {
                result.add(fullInfo);
            }
        }
        return result;
    }

    private BSONObject updateTransitionRemoveWs(LifeCycleConfigFullEntity lifeCycleConfigFullEntity,
            TransitionFullEntity transitionFullEntity, String removeWorkspace) {
        BasicBSONList workspaces = transitionFullEntity.getWorkspaces();
        BasicBSONList newWorkspaces = new BasicBSONList();
        for (Object o : workspaces) {
            String workspace = (String) o;
            if (!removeWorkspace.equals(workspace)) {
                newWorkspaces.add(workspace);
            }
        }
        transitionFullEntity.setWorkspaces(newWorkspaces);

        BasicBSONList transitionConfig = lifeCycleConfigFullEntity.getTransitionConfig();
        BasicBSONList config = new BasicBSONList();
        for (Object o : transitionConfig) {
            BSONObject transition = (BSONObject) o;
            if (transition.containsField(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME)
                    && !transition.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME)
                            .equals(transitionFullEntity.getName())) {
                config.add(transition);
            }
        }
        BSONObject alterTransition = TransitionEntityTranslator.FullInfo
                .toBSONObject(transitionFullEntity);
        config.add(alterTransition);
        lifeCycleConfigFullEntity.setTransitionConfig(config);
        return LifeCycleEntityTranslator.FullInfo.toBSONObject(lifeCycleConfigFullEntity);
    }

    private BSONObject updateTransitionAddWs(LifeCycleConfigFullEntity lifeCycleConfigFullEntity,
            TransitionFullEntity transitionFullEntity, String addWorkspace) {
        BasicBSONList workspaces = transitionFullEntity.getWorkspaces();
        workspaces.add(addWorkspace);

        BasicBSONList transitionConfig = lifeCycleConfigFullEntity.getTransitionConfig();
        BasicBSONList config = new BasicBSONList();
        for (Object o : transitionConfig) {
            BSONObject transition = (BSONObject) o;
            if (transition.containsField(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME)
                    && !transition.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME)
                            .equals(transitionFullEntity.getName())) {
                config.add(transition);
            }
        }
        BSONObject alterTransition = TransitionEntityTranslator.FullInfo
                .toBSONObject(transitionFullEntity);
        config.add(alterTransition);
        lifeCycleConfigFullEntity.setTransitionConfig(config);
        return LifeCycleEntityTranslator.FullInfo.toBSONObject(lifeCycleConfigFullEntity);
    }

    private void removeSchedule(BasicBSONList scheduleIds) throws Exception {
        for (Object scheduleId : scheduleIds) {
            String id = (String) scheduleId;
            scheduleService.deleteSchedule(id, true);
        }
    }

    private SiteInfo getSiteByStageTag(String workspace, String stageTag) throws ScheduleException {
        WorkspaceInfo wsInfo = ScheduleServer.getInstance().getWorkspace(workspace);
        if (wsInfo == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKSPACE_NOT_EXISTS,
                    "workspace not exist, workspace" + workspace);
        }
        List<Integer> siteIdList = wsInfo.getSiteIdList();
        for (Integer siteId : siteIdList) {
            SiteInfo siteInfo = ScheduleServer.getInstance().getSite(siteId);
            if (siteInfo.getStageTag() != null && siteInfo.getStageTag().equals(stageTag)) {
                return siteInfo;
            }
        }
        return null;
    }

    private TransitionScheduleEntity updateTransitionScheduleByGlobal(
            TransitionScheduleEntity oldInfo, TransitionUserEntity updatorInfo,
            String preferredRegion, String preferredZone, String updateUser) {
        String id = oldInfo.getId();
        long time = oldInfo.getCreateTime();
        String user = oldInfo.getCreateUser();
        String workspace = oldInfo.getWorkspace();
        return TransitionEntityTranslator.WsFullInfo.fromTransitionInfo(updatorInfo, id, workspace,
                user, time, true, false, preferredRegion, preferredZone, updateUser,
                new Date().getTime());
    }

    private TransitionScheduleEntity updateTransitionSchedule(TransitionScheduleEntity oldInfo,
            TransitionUserEntity updatorInfo, String preferredRegion, String preferredZone,
            String updateUser) {
        String id = oldInfo.getId();
        long time = oldInfo.getCreateTime();
        String user = oldInfo.getCreateUser();
        String workspace = oldInfo.getWorkspace();
        return TransitionEntityTranslator.WsFullInfo.fromTransitionInfo(updatorInfo, id, workspace,
                user, time, true, true, preferredRegion, preferredZone, updateUser,
                new Date().getTime());
    }

    private TransitionScheduleEntity createTransitionSchedule(TransitionUserEntity entity,
            String user, String workspace, boolean customized, String preferredRegion,
            String preferredZone) {
        Date date = new Date();
        // 更新接口应该判断是不是只改名字，改名字就不需要涉及调度任务重建，需要关联到工作区上的，全局流上面可以记一个id，工作区去关联，后续全局名字变更不需要变更其他工作区
        String id = ScmIdGenerator.ScheduleId.get(date);
        return TransitionEntityTranslator.WsFullInfo.fromTransitionInfo(entity, id, workspace, user,
                date.getTime(), true, customized, preferredRegion, preferredZone, user,
                date.getTime());
    }

    private TransitionUserEntity overwriteTransition(TransitionFullEntity fullInfo,
            TransitionUserEntity customInfo) {
        /**
         * 合并说明，合并
         * name，flow，matcher，transitionTriggers，cleanTriggers，extraContent；不细化到该层级以下
         */
        TransitionFullEntity newInfo = fullInfo.clone();
        if (null != customInfo.getName()) {
            newInfo.setName(customInfo.getName());
        }

        if (null != customInfo.getFlow()) {
            newInfo.setFlow(customInfo.getFlow());
        }

        if (null != customInfo.getTransitionTriggers()) {
            newInfo.setTransitionTriggers(customInfo.getTransitionTriggers());
        }

        if (null != customInfo.getCleanTriggers()) {
            newInfo.setCleanTriggers(customInfo.getCleanTriggers());
        }

        if (null != customInfo.getMatcher()) {
            newInfo.setMatcher(customInfo.getMatcher());
        }

        if (null != customInfo.getExtraContent()) {
            newInfo.setExtraContent(customInfo.getExtraContent());
        }
        else {
            newInfo.setExtraContent(customInfo.getExtraContent());
        }

        return newInfo;
    }

    private boolean wsHadUsedTransition(String workspace, TransitionFullEntity info)
            throws ScheduleException {
        BasicBSONList workspaces = info.getWorkspaces();
        for (Object o : workspaces) {
            String w = (String) o;
            if (workspace.equals(w)) {
                return true;
            }
        }
        TransitionScheduleEntity entity = queryWsTransition(workspace, info.getId());
        return entity != null;
    }

    private TransitionScheduleEntity queryWsTransition(String workspace, String globalTransitionId)
            throws ScheduleException {
        BSONObject ws = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME,
                workspace);
        BSONObject transitionId = new BasicBSONObject(
                FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID, globalTransitionId);
        BasicBSONList obj = new BasicBSONList();
        obj.add(ws);
        obj.add(transitionId);
        BSONObject matcher = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_AND, obj);
        try {
            TransitionScheduleEntity entity = lifeCycleScheduleDao.queryOne(matcher);
            return entity;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to query ws apply transition record", e);
        }
    }

    private List<TransitionScheduleEntity> getNeedUpdateFlowTransition(
            ScmBSONObjectCursor wsTransitionList, String oldStageTag, String newStageTag)
            throws Exception {
        List<TransitionScheduleEntity> result = new ArrayList<>();
        while (wsTransitionList.hasNext()) {
            BSONObject configSchedule = wsTransitionList.next();
            TransitionScheduleEntity entity = TransitionEntityTranslator.WsFullInfo
                    .fromBSONObject(configSchedule);
            TransitionUserEntity transition = entity.getTransition();
            String source = transition.getFlow().getSource();
            String dest = transition.getFlow().getDest();
            if (oldStageTag.equals(source)) {
                transition.setFlow(new ScmFlow(newStageTag, dest));
                result.add(entity);
            }
            else if (oldStageTag.equals(dest)) {
                transition.setFlow(new ScmFlow(source, newStageTag));
                result.add(entity);
            }
        }
        return result;
    }

    private Set<String> getStageTagSet(LifeCycleConfigFullEntity info) {
        Set<String> set = new HashSet<>();
        BasicBSONList stageTagConfig = info.getStageTagConfig();
        for (Object o : stageTagConfig) {
            BSONObject stageTag = (BSONObject) o;
            set.add((String) stageTag.get(FieldName.LifeCycleConfig.FIELD_STAGE_NAME));
        }
        return set;
    }

    private TransitionFullEntity getTransitionById(LifeCycleConfigFullEntity config, String id) {
        for (Object o : config.getTransitionConfig()) {
            BSONObject transition = (BSONObject) o;
            TransitionFullEntity entity = TransitionEntityTranslator.FullInfo
                    .fromBSONObject(transition);
            if (id.equals(entity.getId())) {
                return entity;
            }
        }
        return null;
    }

    private TransitionFullEntity getTransitionByName(LifeCycleConfigFullEntity config,
            String transitionName) {
        for (Object o : config.getTransitionConfig()) {
            BSONObject transition = (BSONObject) o;
            TransitionFullEntity entity = TransitionEntityTranslator.FullInfo
                    .fromBSONObject(transition);
            if (transitionName.equals(entity.getName())) {
                return entity;
            }
        }
        return null;
    }

    private boolean stageTagUsedBySite(String stageTagName) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Site.FIELD_STAGE_TAG, stageTagName);
        ScmBSONObjectCursor siteCursor = null;
        try {
            siteCursor = siteDao.query(matcher);
            return siteCursor.hasNext();
        }
        finally {
            if (siteCursor != null) {
                siteCursor.close();
            }
        }
    }

    private void checkUsedByTransition(String stageTagName, BasicBSONList transitionConfig)
            throws ScheduleException {
        if (transitionConfig == null) {
            return;
        }
        for (Object o : transitionConfig) {
            TransitionFullEntity transition = TransitionEntityTranslator.FullInfo
                    .fromBSONObject((BSONObject) o);
            String source = transition.getFlow().getSource();
            String dest = transition.getFlow().getDest();
            if (stageTagName.equals(source) || stageTagName.equals(dest)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "remove stage tag failed, the stage tag already used by transition, stageTag="
                                + stageTagName + ", transitionName=" + transition.getName());
            }
        }
    }

    private boolean isSystemStageTag(String stageTagName) {
        return stageTagName.equals(LifeCycleConfigDefine.ScmSystemStageTagType.HOT)
                || stageTagName.equals(LifeCycleConfigDefine.ScmSystemStageTagType.WARM)
                || stageTagName.equals(LifeCycleConfigDefine.ScmSystemStageTagType.COLD);
    }

    private boolean stageTagExist(LifeCycleConfigFullEntity info, String stageTageName) {
        BasicBSONList stageTagConfig = info.getStageTagConfig();
        for (Object o : stageTagConfig) {
            BSONObject stageTag = (BSONObject) o;
            String existStageTagName = (String) stageTag
                    .get(FieldName.LifeCycleConfig.FIELD_STAGE_NAME);
            if (stageTageName.equals(existStageTagName)) {
                return true;
            }
        }
        return false;
    }

    private void configUsedBySite() throws Exception {
        ScmBSONObjectCursor siteCursor = null;
        try {
            siteCursor = siteDao.query(new BasicBSONObject());
            while (siteCursor.hasNext()) {
                BSONObject obj = siteCursor.next();
                SiteEntity siteEntity = ConfigEntityTranslator.Site.fromBSONObject(obj);
                String stageTag = siteEntity.getStageTag();
                if (StringUtils.hasText(stageTag) && !isSystemStageTag(stageTag)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_USED,
                            "old life cycle config had used by site=" + siteEntity.getName()
                                    + "stageTag=" + stageTag);
                }
            }
        }
        finally {
            if (null != siteCursor) {
                siteCursor.close();
            }
        }
    }

    private void configUsedByWs() throws Exception {
        ScmBSONObjectCursor lifeCycleScheduleCursor = null;
        try {
            lifeCycleScheduleCursor = lifeCycleScheduleDao.query(null);
            if (lifeCycleScheduleCursor.hasNext()) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.LIFE_CYCLE_CONFIG_USED,
                        "old life cycle config had used by workspace");
            }
        }
        finally {
            if (null != lifeCycleScheduleCursor) {
                lifeCycleScheduleCursor.close();
            }
        }
    }

    private void checkStageTagAndTransition(LifeCycleConfigFullEntity info)
            throws ScheduleException {
        BasicBSONList stageTagConfig = info.getStageTagConfig();
        BasicBSONList transitionConfig = info.getTransitionConfig();

        Set<String> stageTagNameSet = new HashSet<>();
        for (Object o : stageTagConfig) {
            BasicBSONObject stageTag = (BasicBSONObject) o;
            if (stageTag != null) {
                String name = BsonUtils.getStringChecked(stageTag,
                        FieldName.LifeCycleConfig.FIELD_STAGE_NAME);
                if (stageTagNameSet.contains(name)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.REPEAT_STAGE_TAG_NAME,
                            "stage tag name repeat, stageTag=" + name);
                }
                stageTagNameSet.add(name);
            }
        }
        addSystemStageTag(stageTagConfig, stageTagNameSet);

        Set<String> transitionNameSet = new HashSet<>();
        for (Object o : transitionConfig) {
            BasicBSONObject transition = (BasicBSONObject) o;
            if (transition != null) {
                TransitionUserEntity userEntity = TransitionEntityTranslator.UserInfo
                        .fromBSONObject(transition);
                checkTransition(userEntity, stageTagNameSet);
                String name = userEntity.getName();

                if (transitionNameSet.contains(name)) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "transition name repeat, transitionName=" + name);
                }
                transitionNameSet.add(name);
            }
        }
    }

    // 应该叫添加系统阶段标签
    private void addSystemStageTag(BasicBSONList stageTagConfig, Set<String> stageTagNameSet) {
        if (!stageTagNameSet.contains(LifeCycleConfigDefine.ScmSystemStageTagType.HOT)) {
            BasicBSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME,
                    LifeCycleConfigDefine.ScmSystemStageTagType.HOT);
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC,
                    LifeCycleConfigDefine.ScmSystemStageTagType.HOT);
            stageTagConfig.add(obj);
            stageTagNameSet.add(LifeCycleConfigDefine.ScmSystemStageTagType.HOT);
        }
        if (!stageTagNameSet.contains(LifeCycleConfigDefine.ScmSystemStageTagType.WARM)) {
            BasicBSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME,
                    LifeCycleConfigDefine.ScmSystemStageTagType.WARM);
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC,
                    LifeCycleConfigDefine.ScmSystemStageTagType.WARM);
            stageTagConfig.add(obj);
            stageTagNameSet.add(LifeCycleConfigDefine.ScmSystemStageTagType.WARM);
        }
        if (!stageTagNameSet.contains(LifeCycleConfigDefine.ScmSystemStageTagType.COLD)) {
            BasicBSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME,
                    LifeCycleConfigDefine.ScmSystemStageTagType.COLD);
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC,
                    LifeCycleConfigDefine.ScmSystemStageTagType.COLD);
            stageTagConfig.add(obj);
            stageTagNameSet.add(LifeCycleConfigDefine.ScmSystemStageTagType.COLD);
        }
    }

    public void checkTransition(TransitionUserEntity info, Set<String> stageTagNameSet)
            throws ScheduleException {
        if (!StringUtils.hasText(info.getName())) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition name can not be null,name=" + info.getName());
        }
        checkArgNotNull("flow", info.getFlow());
        checkArgNotNull("transition_triggers", info.getTransitionTriggers());
        checkArgNotNull("extra_content", info.getExtraContent());

        // check flow
        try {
            checkFlow(info.getFlow().getSource(), info.getFlow().getDest(), stageTagNameSet);
        }
        catch (ScheduleException e) {
            logger.error("transition flow is valid, transitionName=" + info.getName());
            throw e;
        }

        // check transition_triggers
        checkTransitionTriggers(info.getTransitionTriggers());

        // check clean triggers
        ScmCleanTriggers cleanTriggers = info.getCleanTriggers();
        if (cleanTriggers != null) {
            checkCleanTriggers(cleanTriggers);
        }

        // check extra content
        checkExtraContent(info.getExtraContent());

        // checkMatcher
        checkMatcher(info.getMatcher(), info.getExtraContent().getScope());
    }

    private void checkMatcher(BSONObject matcher, String scope) throws ScheduleException {
        int scopeType = LifeCycleCommonTools.getScopeType(scope);
        if (scopeType == ScheduleDefine.ScopeType.CURRENT) {
            // no check for current scope
            return;
        }
        if (scopeType != ScheduleDefine.ScopeType.HISTORY
                && scopeType != ScheduleDefine.ScopeType.ALL) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "unknown scope:scope=" + scope);
        }
        ScmArgChecker.ExtralCondition.checkHistoryFileMatcher(matcher);
    }

    private void checkExtraContent(ScmExtraContent extraContent) throws ScheduleException {
        String dataCheckLevel = extraContent.getDataCheckLevel();
        String scope = extraContent.getScope();
        if (!dataCheckLevel.equals(ScheduleDefine.DataCheckLevel.STRICT)
                && !dataCheckLevel.equals(ScheduleDefine.DataCheckLevel.WEEK)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "extra content dataCheckLevel may choose strict or week, dataCheckLevel="
                            + dataCheckLevel);
        }

        if (!scope.equals("ALL") && !scope.equals("CURRENT") && !scope.equals("HISTORY")) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "extra content scope may choose ALL or CURRENT or HISTORY,scope=" + scope);
        }
    }

    private void checkCleanTriggers(ScmCleanTriggers cleanTriggers) throws ScheduleException {
        String mode = cleanTriggers.getMode();
        long maxExecTime = cleanTriggers.getMaxExecTime();
        String rule = cleanTriggers.getRule();
        List<ScmCleanTriggers.Trigger> triggerList = cleanTriggers.getTriggerList();

        checkMode(mode);

        if (maxExecTime < 0) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "clean maxExecTime can not less than 0,maxExecTime=" + maxExecTime);
        }

        checkRuleValid(rule);

        Set<String> triggerIdSet = new HashSet<>();
        for (ScmCleanTriggers.Trigger trigger : triggerList) {
            String id = trigger.getId();
            checkCleanTrigger(trigger);
            if (triggerIdSet.contains(id)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.REPEAT_TRIGGER_ID,
                        "Invalid the trigger ID=" + id);
            }
            triggerIdSet.add(id);
        }
    }

    private void checkCleanTrigger(ScmCleanTriggers.Trigger trigger) throws ScheduleException {
        String mode = trigger.getMode();
        String lastAccessTime = trigger.getLastAccessTime();
        String transitionTime = trigger.getTransitionTime();

        checkMode(mode);
        checkTimeValid("clean trigger lastAccessTime", lastAccessTime);
        checkTimeValid("clean trigger transitionTime", transitionTime);
    }

    private void checkTransitionTriggers(ScmTransitionTriggers transitionTriggers)
            throws ScheduleException {
        String mode = transitionTriggers.getMode();
        long maxExecTime = transitionTriggers.getMaxExecTime();
        String rule = transitionTriggers.getRule();
        List<ScmTransitionTriggers.Trigger> triggerList = transitionTriggers.getTriggerList();

        checkMode(mode);

        if (maxExecTime < 0) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition maxExecTime can not less than 0,maxExecTime=" + maxExecTime);
        }

        checkRuleValid(rule);

        if (triggerList.size() < 1) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition trigger can not be null, triggerList=" + triggerList);
        }
        Set<String> triggerIdSet = new HashSet<>();
        for (ScmTransitionTriggers.Trigger trigger : triggerList) {
            String id = trigger.getId();
            checkTransitionTrigger(trigger);
            // check id repeat
            if (triggerIdSet.contains(id)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.REPEAT_TRIGGER_ID,
                        "Invalid the trigger id=" + id);
            }
            triggerIdSet.add(id);
        }
    }

    private void checkTransitionTrigger(ScmTransitionTriggers.Trigger trigger)
            throws ScheduleException {
        String mode = trigger.getMode();
        String createTime = trigger.getCreateTime();
        String lastAccessTime = trigger.getLastAccessTime();
        String buildTime = trigger.getBuildTime();

        checkMode(mode);
        checkTimeValid("transition trigger createTime", createTime);
        checkTimeValid("transition trigger lastAccessTime", lastAccessTime);
        checkTimeValid("transition trigger buildTime", buildTime);
    }

    private void checkTimeValid(String timeName, String time) throws ScheduleException {
        ScheduleCommonTools.checkAndParseTime(timeName, time);
    }

    private void checkRuleValid(String rule) throws ScheduleException {
        if (!CronExpression.isValidExpression(rule)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition rule is invalid cron expression, rule=" + rule);
        }
    }

    private void checkMode(String mode) throws ScheduleException {
        if (!StringUtils.hasText(mode)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition mode can not be null, mode=" + mode);
        }
        if (!mode.equals(ScheduleDefine.ModeType.ALL)
                && !mode.equals(ScheduleDefine.ModeType.ANY)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition mode may choose all or any,mode=" + mode);
        }
    }

    private void checkFlow(String source, String dest, Set<String> stageTagNameSet)
            throws ScheduleException {
        if (!StringUtils.hasText(source)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "flow source can not be null, source=" + source);
        }
        if (!StringUtils.hasText(dest)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "flow dest can not be null, dest=" + dest);
        }
        if (source.equals(dest)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "flow source and dest can not be the same, source=" + source + ", dest="
                            + dest);
        }
        if (!stageTagNameSet.contains(source)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "flow source not in the stage tag list, source=" + source);
        }
        if (!stageTagNameSet.contains(dest)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "flow dest not in the stage tag list, dest=" + dest);
        }
    }

    private void checkArgNotNull(String argName, Object argValue) throws ScheduleException {
        if (argValue == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "transition " + argName + "can not be null");
        }
    }

    private LifeCycleConfigFullEntity createConfigFullEntity(String username,
            LifeCycleConfigUserEntity userInfo) {
        Date date = new Date();
        return LifeCycleEntityTranslator.FullInfo.fromUserInfo(userInfo, username,
                date.getTime());
    }

    private ScmLock lockGlobal() throws ScmLockException {
        return scmLockManager.acquiresLock(LifeCycleCommonDefine.GLOBAL_LIFE_CYCLE_LOCK_PATH);
    }

    private void unLock(ScmLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }
}
