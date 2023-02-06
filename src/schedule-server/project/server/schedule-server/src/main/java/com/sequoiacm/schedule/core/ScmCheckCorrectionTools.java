package com.sequoiacm.schedule.core;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.LifeCycleCommonTools;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.LifeCycleConfigFullEntity;
import com.sequoiacm.schedule.common.model.LifeCycleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.common.model.ScmCleanTriggers;
import com.sequoiacm.schedule.common.model.ScmExtraContent;
import com.sequoiacm.schedule.common.model.ScmFlow;
import com.sequoiacm.schedule.common.model.ScmTransitionTriggers;
import com.sequoiacm.schedule.common.model.TransitionEntityTranslator;
import com.sequoiacm.schedule.common.model.TransitionFullEntity;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.common.model.TransitionUserEntity;
import com.sequoiacm.schedule.core.meta.SiteInfo;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.dao.LifeCycleConfigDao;
import com.sequoiacm.schedule.dao.LifeCycleScheduleDao;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScmCheckCorrectionTools {
    private static final Logger logger = LoggerFactory.getLogger(ScmCheckCorrectionTools.class);

    public static final String GLOBAL_LIFE_CYCLE = "global_life_cycle";

    private static ScmCheckCorrectionTools instance = new ScmCheckCorrectionTools();

    private LifeCycleConfigDao lifeCycleConfigDao;

    private LifeCycleScheduleDao lifeCycleScheduleDao;

    private ScheduleDao scheduleDao;

    private ScmLockManager scmLockManager;

    private ScmCheckCorrectionTools() {

    }

    public static ScmCheckCorrectionTools getInstance() {
        return instance;
    }

    public void init(LifeCycleConfigDao lifeCycleConfigDao,
            LifeCycleScheduleDao lifeCycleScheduleDao, ScheduleDao scheduleDao,
            ScmLockManager scmLockManager) {
        this.lifeCycleConfigDao = lifeCycleConfigDao;
        this.lifeCycleScheduleDao = lifeCycleScheduleDao;
        this.scheduleDao = scheduleDao;
        this.scmLockManager = scmLockManager;
    }

    public void checkAndCorrection() throws Exception {
        ScmLock lock = null;
        try {
            lock = lockGlobal();
            logger.info("###############check and correction life cycle config start#############");
            BSONObject config = lifeCycleConfigDao.queryOne();
            if (config == null) {
                logger.info("life cycle config is null, skip to check and correction");
                return;
            }
            LifeCycleConfigFullEntity lifeCycleConfig = LifeCycleEntityTranslator.FullInfo
                    .fromBSONObject(config);

            // 检查全局流和直接引用的工作区流信息是否一致
            correctionGlobalWithWs(lifeCycleConfig);

            // 存放多余的流和无流关联的调度任务，即没有流与他关联，会进行删除
            BasicBSONList moreScheduleIds = new BasicBSONList();

            // 检查工作区流和调度任务
            checkWsTransitionAndSchedule(moreScheduleIds);

            // 删除多余的流和无流关联的调度任务
            deleteMoreSchedule(moreScheduleIds);

            logger.info("###############check and correction life cycle config end#############");
        }
        catch (Exception e) {
            logger.error("check correction life cycle config have error", e);
            logger.info("###############check and correction life cycle config end#############");
            throw e;
        }
        finally {
            if (null != lock) {
                lock.unlock();
            }
        }
    }

    private String getSourceSiteName(String scheduleId) throws Exception {
        ScheduleFullEntity scheduleFullEntity = scheduleDao.queryOne(scheduleId);
        if (scheduleFullEntity == null) {
            logger.error("can not found schedule by id,scheduleId={}", scheduleId);
            return null;
        }
        String source;
        String type = scheduleFullEntity.getType();
        if (type.equals("move_file") || type.equals("copy_file")) {
            source = (String) scheduleFullEntity.getContent()
                    .get(FieldName.Schedule.FIELD_COPY_SOURCE_SITE);
        }
        else if (type.equals("clean_file")) {
            source = (String) scheduleFullEntity.getContent()
                    .get(FieldName.Schedule.FIELD_CLEAN_SITE);
        }
        else {
            logger.error("schedule type is invalid,can not correction flow");
            return null;
        }
        return source;
    }

    private String getDestSiteName(String scheduleId) throws Exception {
        ScheduleFullEntity scheduleFullEntity = scheduleDao.queryOne(scheduleId);
        if (scheduleFullEntity == null){
            logger.error("can not found schedule by id,scheduleId={}", scheduleId);
            return null;
        }
        String dest;
        String type = scheduleFullEntity.getType();
        if (type.equals("move_file") || type.equals("copy_file")) {
            dest = (String) scheduleFullEntity.getContent()
                    .get(FieldName.Schedule.FIELD_COPY_TARGET_SITE);
        }
        else if (type.equals("clean_file")) {
            dest = (String) scheduleFullEntity.getContent()
                    .get(FieldName.Schedule.FIELD_CLEAN_CHECK_SITE);
        }
        else {
            logger.error("schedule type is invalid,can not correction flow");
            return null;
        }
        return dest;
    }

    private BasicBSONList createSchedule(TransitionScheduleEntity oldInfo, String sourceName,
            String destName) throws ScheduleException {
        if (oldInfo.getTransition().getCleanTriggers() != null) {
            BasicBSONList ids = new BasicBSONList();
            try {
                ScheduleUserEntity copyScheduleEntity = LifeCycleCommonTools
                        .createScheduleUserEntity(ScheduleDefine.ScheduleType.COPY_FILE, oldInfo,
                                sourceName, destName, oldInfo.getPreferredRegion(),
                                oldInfo.getPreferredZone(), new Date());
                copyScheduleEntity.setDesc("rebuild transition copy schedule");
                ScheduleFullEntity newCopySchedule = ScheduleMgrWrapper.getInstance()
                        .createSchedule(oldInfo.getCreateUser(), copyScheduleEntity);
                ids.add(newCopySchedule.getId());
            }
            catch (Exception e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to rebuild workspace transition,workspace=" + oldInfo.getWorkspace()
                                + "transitionId=" + oldInfo.getId(),
                        e);
            }

            try {
                ScheduleUserEntity cleanScheduleEntity = LifeCycleCommonTools
                        .createScheduleUserEntity(ScheduleDefine.ScheduleType.CLEAN_FILE, oldInfo,
                                sourceName, destName, oldInfo.getPreferredRegion(),
                                oldInfo.getPreferredZone(), new Date());
                cleanScheduleEntity.setDesc("rebuild transition clean schedule");
                ScheduleFullEntity newCleanSchedule = ScheduleMgrWrapper.getInstance()
                        .createSchedule(oldInfo.getCreateUser(), cleanScheduleEntity);
                ids.add(newCleanSchedule.getId());
            }
            catch (Exception e) {
                removeScheduleSilence(ids);
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to rebuild workspace transition,workspace=" + oldInfo.getWorkspace()
                                + "transitionId=" + oldInfo.getId(),
                        e);
            }
            return ids;
        }
        else {
            BasicBSONList ids = new BasicBSONList();
            try {
                ScheduleUserEntity moveScheduleEntity = LifeCycleCommonTools
                        .createScheduleUserEntity(ScheduleDefine.ScheduleType.MOVE_FILE, oldInfo,
                                sourceName, destName, oldInfo.getPreferredRegion(),
                                oldInfo.getPreferredZone(), new Date());
                moveScheduleEntity.setDesc("rebuild transition move schedule");
                ScheduleFullEntity newSchedule = ScheduleMgrWrapper.getInstance()
                        .createSchedule("admin", moveScheduleEntity);
                ids.add(newSchedule.getId());
            }
            catch (Exception e) {
                removeScheduleSilence(ids);
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to rebuild workspace transition,workspace=" + oldInfo.getWorkspace()
                                + "transitionId=" + oldInfo.getId(),
                        e);
            }
            return ids;
        }
    }

    private void deleteMoreSchedule(BasicBSONList moreScheduleList) throws Exception {
        if (moreScheduleList.size() == 0) {
            return;
        }
        logger.info("delete more schedule,scheduleIdList={}", moreScheduleList);
        removeSchedule(moreScheduleList);
    }

    private void checkWsTransitionAndSchedule(BasicBSONList moreScheduleIds) throws Exception {
        // ws transition ws schedule
        logger.info("#############check workspace transition with schedule#################");
        ScmBSONObjectCursor cursor = null;
        try {
            // 按照工作区名字排序查询工作区流
            cursor = lifeCycleScheduleDao.query(new BasicBSONObject(),
                    new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME, 1));
            // 标记工作区是否发生了变化，发生了变化需要去查询新工作区的调度任务列表
            String workspaceMark = "";
            Set<String> wsScheduleIdSet = new HashSet<>();
            while (cursor.hasNext()) {
                BSONObject obj = cursor.next();
                TransitionScheduleEntity transitionSchedule = TransitionEntityTranslator.WsFullInfo
                        .fromBSONObject(obj);
                String workspace = transitionSchedule.getWorkspace();
                if (!workspace.equals(workspaceMark)) {
                    // 在获取新的工作区的调度任务列表之前，将剩余的调度任务列表内容，放到多余的待删除调度任务列表里，原因是与流存在实际关联的已经被移除了，
                    // 剩下的多余的，即没有实际的流的调度列表与此关联
                    moreScheduleIds.addAll(wsScheduleIdSet);
                    // 工作区发生变化，获取新工作区的调度任务列表
                    workspaceMark = workspace;
                    wsScheduleIdSet = getWsScheduleIdSet(workspace);
                }
                // 检查并修正flow中的source和dest信息
                checkAndCorrectionFlowStageTag(transitionSchedule);
                // 检查工作区流和调度任务的关系，如果工作区流里的调度任务缺失，则去重建新的调度任务
                checkAndCorrectionWsTransition(transitionSchedule.getId(), wsScheduleIdSet,
                        moreScheduleIds);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void checkAndCorrectionWsTransition(String id, Set<String> wsScheduleIdSet,
            BasicBSONList moreScheduleIds) throws Exception {
        logger.info(
                "###########check and correction workspace transition,transitionId={} #############",
                id);
        // 拿取最新的流信息
        TransitionScheduleEntity entity;
        try {
            entity = lifeCycleScheduleDao.queryOne(new BasicBSONObject(
                    FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID, id));
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not get ws transition info,can not check and correction!transitionId="
                            + id,
                    e);
        }
        if (entity == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "workspace transition not found!transitionId=" + id);
        }
        boolean correction = false;
        BasicBSONList scheduleIds = entity.getScheduleIds();
        if (scheduleIds == null || scheduleIds.size() == 0) {
            // 流的调度任务列表是空的，需要重建
            correction = true;
        }
        else {
            for (Object o : scheduleIds) {
                String scheduleId = (String) o;
                if (!wsScheduleIdSet.contains(scheduleId)) {
                    // 如果流的调度任务在工作区的调度列表里没有，则说明该流的调度任务实际不存在，需要重建
                    correction = true;
                }
                // 将检查过的流的调度任务移除，最后剩下的是没有流与之关联的流调度任务
                wsScheduleIdSet.remove(scheduleId);
            }
        }

        // 需要修正
        if (correction) {
            // 如果需要重建，旧的调度任务需要删除，放到多余的待删除调度任务列表里；
            moreScheduleIds.addAll(scheduleIds);
            correctionWsTransition(entity, moreScheduleIds);
        }
    }

    private void correctionWsTransition(TransitionScheduleEntity info,
            BasicBSONList moreScheduleIds) throws Exception {
        // 把旧的删掉,若删除失败，再最后删除多余的列表里，会继续进行删除，如果还是失败，会在下次检查和修正时被删除
        removeSchedule(info.getScheduleIds());
        String workspace = info.getWorkspace();
        String source = info.getTransition().getFlow().getSource();
        String dest = info.getTransition().getFlow().getDest();
        SiteInfo sourceSite = getSite(workspace, source);
        SiteInfo destSite = getSite(workspace, dest);
        if (sourceSite == null || destSite == null) {
            // 站点不存在，则该流修正失败，无法创建新的调度任务
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to correction workspace transition,can not get site from workspace,workspace="
                            + workspace + ", sourceStageTag=" + source + " ,destStageTag=" + dest);
        }
        BasicBSONList scheduleIds = new BasicBSONList();
        scheduleIds = createSchedule(info, sourceSite.getName(), destSite.getName());

        info.setScheduleIds(scheduleIds);
        info.setUpdateUser("admin");
        info.setUpdateTime(new Date().getTime());
        try {
            lifeCycleScheduleDao.update(TransitionEntityTranslator.WsFullInfo.toBSONObject(info),
                    null);
        }
        catch (Exception e) {
            logger.error(
                    "failed to correction workspace transition,can not update new transition info,workspace={},sourceStageTag={},destStageTag={}",
                    workspace, source, dest, e);
            removeScheduleSilence(scheduleIds);
            throw e;
        }
        logger.info("correction workspace transition success,new scheduleIds={}", scheduleIds);
    }

    private void checkAndCorrectionFlowStageTag(TransitionScheduleEntity info) throws Exception {
        logger.info("###############check workspace transition flow and site#############");
        /**
         * 检查工作区流的flow中source和dest是否正确； 1、先检查source和dest对应的站点是否存在，不存在则是有问题的flow；
         * 2、如果1存在，检查和调度任务中的source和dest是否对应的上，对应补上，则是有问题的flow
         * flow最终结果以调度任务的source和dest为准，若从调度任务获取不到source和dest，则该流回滚不了；
         * 若更新flow失败，则不做后续的修正工作区流操作，原因是flow有问题，修正工作区流新建的调度任务也是有问题的
         */
        String flowSource = info.getTransition().getFlow().getSource();
        String flowDest = info.getTransition().getFlow().getDest();
        String workspace = info.getWorkspace();
        SiteInfo sourceSite = getSite(workspace, flowSource);
        SiteInfo destSite = getSite(workspace, flowDest);
        // 从调度任务里面获取源站点和目标站点的名字
        BasicBSONList scheduleIds = info.getScheduleIds();
        String source = getSource(scheduleIds);
        String dest = getDest(scheduleIds);
        if (source == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not correction flow,because can not get source sitename from schedule ,transitionId="
                            + info.getId());
        }
        if (dest == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not correction flow,because can not get dest sitename from schedule ,transitionId="
                            + info.getId());
        }
        // flow 中 source的阶段标签对应的站点不存在 或者 调度任务信息里的站点与 flow 中 source 对应的站点不一致
        if (sourceSite == null || !source.equals(sourceSite.getName())) {
            logger.warn(
                    "flow source site different with schedule source site ,will be correction.transitionId={}",
                    info.getId());
            correctionFlowSuccess(info, source, dest);
        }

        if (destSite == null || !dest.equals(destSite.getName())) {
            logger.warn(
                    "flow dest site different with schedule dest site ,will be correction.transitionId={}",
                    info.getId());
            correctionFlowSuccess(info, source, dest);
        }
    }

    private void correctionFlowSuccess(TransitionScheduleEntity info, String source, String dest)
            throws ScheduleException {
        logger.info(
                "###################### correction flow start,workspace={},transitionId={} ####################",
                info.getWorkspace(), info.getId());
        SiteInfo sourceSite = ScheduleServer.getInstance().getSite(source);
        SiteInfo destSite = ScheduleServer.getInstance().getSite(dest);
        if (sourceSite == null || destSite == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "correction flow failed,can not get sourceStageTag destStageTag from site,sourceSiteName="
                            + source + ", destSiteName=" + dest + ", transitionId=" + info.getId());
        }
        if (!StringUtils.hasText(sourceSite.getStageTag())
                || !StringUtils.hasText(destSite.getStageTag())) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "correction flow failed,can not get sourceStageTag destStageTag from site,sourceSiteName="
                            + source + ", destSiteName=" + dest + ", transitionId=" + info.getId());
        }
        info.getTransition().setFlow(new ScmFlow(sourceSite.getStageTag(), destSite.getStageTag()));
        info.setUpdateUser("admin");
        info.setUpdateTime(new Date().getTime());
        try {
            lifeCycleScheduleDao.update(TransitionEntityTranslator.WsFullInfo.toBSONObject(info),
                    null);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "correction flow failed,sourceSiteStageTag=" + sourceSite.getStageTag()
                            + ", destSiteStageTag=" + destSite.getStageTag() + ", transitionId="
                            + info.getId(),
                    e);
        }
        logger.info(
                "###################### correction flow success,workspace={},transitionId={},newFlow={} ####################",
                info.getWorkspace(), info.getId(),
                sourceSite.getStageTag() + "," + destSite.getStageTag());
    }

    private String getSource(BasicBSONList ids) {
        // 从调度任务中获取源站点名
        for (Object id : ids) {
            String sourceSiteName = null;
            try {
                sourceSiteName = getSourceSiteName((String) id);
            }
            catch (Exception e) {
                logger.error("can not get source from schedule,scheduleId={}", id, e);
            }
            if (sourceSiteName != null) {
                return sourceSiteName;
            }
        }
        return null;
    }

    private String getDest(BasicBSONList ids) {
        // 从调度任务中获取目的站点名
        for (Object id : ids) {
            String destSiteName = null;
            try {
                destSiteName = getDestSiteName((String) id);
            }
            catch (Exception e) {
                logger.error("can not get source from schedule,scheduleId={}", id, e);
            }
            if (destSiteName != null) {
                return destSiteName;
            }
        }
        return null;
    }

    private SiteInfo getSite(String workspace, String stageTag) {
        WorkspaceInfo wsInfo = ScheduleServer.getInstance().getWorkspace(workspace);
        List<Integer> siteIdList = wsInfo.getSiteIdList();
        for (Integer siteId : siteIdList) {
            SiteInfo siteInfo = ScheduleServer.getInstance().getSite(siteId);
            if (siteInfo.getStageTag() != null && siteInfo.getStageTag().equals(stageTag)) {
                return siteInfo;
            }
        }
        return null;
    }

    private Set<String> getWsScheduleIdSet(String workspace) throws Exception {
        Set<String> idSet = new HashSet<>();
        BSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_WORKSPACE, workspace);
        ScmBSONObjectCursor cursor = null;
        try {
            cursor = scheduleDao.query(matcher);
            while (cursor.hasNext()) {
                BSONObject obj = cursor.next();
                ScheduleFullEntity schedule = ScheduleEntityTranslator.FullInfo.fromBSONObject(obj);
                if (schedule.getTransitionId() != null) {
                    idSet.add(schedule.getId());
                }
            }
            return idSet;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void correctionGlobalWithWs(LifeCycleConfigFullEntity lifeCycleConfig)
            throws Exception {
        /**
         * 边检查边修复； 检查和修正全局流和工作区直接引用流：检查方式，以全局流为基础，比对全局流和工作区流信息是否一致；
         * 检查的信息包括flow中的source和dest，transitionTriggers中的每一个项，cleanTriggers中的每一项，
         * extraContent和matcher的信息； 修正过程：删除工作区流旧的调度任务-》更新工作区流信息-》利用新信息创建调度任务
         */
        logger.info(
                "#############check and correction global with workspace unCustomized transition#################");
        BasicBSONList transitionConfig = lifeCycleConfig.getTransitionConfig();
        for (Object o : transitionConfig) {
            TransitionFullEntity fullEntity = TransitionEntityTranslator.FullInfo
                    .fromBSONObject((BSONObject) o);
            String id = fullEntity.getId();
            ScmBSONObjectCursor cursor = null;
            try {
                cursor = lifeCycleScheduleDao.query(
                        new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID, id));
                while (cursor.hasNext()) {
                    BSONObject next = cursor.next();
                    TransitionScheduleEntity transitionSchedule = TransitionEntityTranslator.WsFullInfo
                            .fromBSONObject(next);
                    if (!transitionSchedule.getCustomized()) {
                        logger.info(
                                "check global and workspace unCustomized transition, globalTransitionId={},wsTransitionId={}",
                                fullEntity.getId(), transitionSchedule.getId());
                        if (!isSameTransitionInfo(fullEntity, transitionSchedule.getTransition())) {
                            logger.warn(
                                    "wsTransition will be correction,wsTransitionName={},wsTransitionId={}",
                                    transitionSchedule.getTransition().getName(),
                                    transitionSchedule.getId());
                            // 检查不一致，开始修正
                            // 删除旧的调度任务
                            logger.info("remove ws transition schedule,scheduleids={}",
                                    transitionSchedule.getScheduleIds());
                            removeSchedule(transitionSchedule.getScheduleIds());
                            // 用全局流信息更新数据
                            TransitionScheduleEntity newInfo = updateWsTransitionFromGlobal(
                                    fullEntity, transitionSchedule);
                            // 利用新的信息创建调度任务
                            BasicBSONList newScheduleIds = createNewSchedule(newInfo);
                            newInfo.setScheduleIds(newScheduleIds);
                            lifeCycleScheduleDao.update(
                                    TransitionEntityTranslator.WsFullInfo.toBSONObject(newInfo),
                                    null);
                        }
                    }
                }
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private BasicBSONList createNewSchedule(TransitionScheduleEntity newInfo)
            throws ScheduleException {
        TransitionUserEntity transition = newInfo.getTransition();
        SiteInfo sourceSite = getSite(newInfo.getWorkspace(), transition.getFlow().getSource());
        SiteInfo destSite = getSite(newInfo.getWorkspace(), transition.getFlow().getDest());
        if (sourceSite == null || destSite == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not create new schedule,sourceSite or destSite not found in workspace,workspace="
                            + newInfo.getWorkspace() + ", sourceStageTag="
                            + transition.getFlow().getSource() + ", destStageTag="
                            + transition.getFlow().getDest());
        }
        return createSchedule(newInfo, sourceSite.getName(), destSite.getName());
    }

    private TransitionScheduleEntity updateWsTransitionFromGlobal(TransitionUserEntity globalInfo,
            TransitionScheduleEntity info) throws Exception {
        info.setTransition(globalInfo);
        info.setScheduleIds(new BasicBSONList());
        info.setUpdateUser("admin");
        info.setUpdateTime(new Date().getTime());
        lifeCycleScheduleDao.update(TransitionEntityTranslator.WsFullInfo.toBSONObject(info), null);
        return info;
    }

    private boolean isSameTransitionInfo(TransitionFullEntity globalTransition,
            TransitionUserEntity wsTransition) {
        // 比较flow中的source是否一致
        if (isDifferentStageTag(globalTransition.getFlow().getSource(),
                wsTransition.getFlow().getSource())) {
            logger.info("compare flow source StageTag different,globalSource={},wsSource={}",
                    globalTransition.getFlow().getSource(), wsTransition.getFlow().getSource());
            return false;
        }

        // 比较flow中的dest是否一致
        if (isDifferentStageTag(globalTransition.getFlow().getDest(),
                wsTransition.getFlow().getDest())) {
            logger.info("compare flow dest StageTag different,globalDest={},wsDest={}",
                    globalTransition.getFlow().getDest(), wsTransition.getFlow().getDest());
            return false;
        }

        // 比较matcher信息是否一致
        if (isDifferentMatcher(globalTransition.getMatcher(), wsTransition.getMatcher())) {
            return false;
        }

        // 比较 extracontent 信息是否一致
        if (isDifferentExtraContent(globalTransition.getExtraContent(),
                wsTransition.getExtraContent())) {
            return false;
        }

        // 比较transitionTriggers信息是否一致
        if (!isSameTransitionTriggers(globalTransition.getTransitionTriggers(),
                wsTransition.getTransitionTriggers())) {
            return false;
        }

        // 比较cleanTriggers信息是否一致
        return isSameCleanTriggers(globalTransition.getCleanTriggers(),
                wsTransition.getCleanTriggers());
    }

    private boolean isDifferentExtraContent(ScmExtraContent globalExtraContent,
            ScmExtraContent wsExtraContent) {
        // 比较 dataCheckLevel 是否一致
        if (isDifferentString(globalExtraContent.getDataCheckLevel(),
                wsExtraContent.getDataCheckLevel())) {
            logger.warn(
                    "compare extraContent dataCheckLevel different,globalExtraContent={},wsExtraContent={}",
                    globalExtraContent.toBSONObj(), wsExtraContent.toBSONObj());
            return true;
        }
        // 比较 scope 是否一致
        if (isDifferentString(globalExtraContent.getScope(), wsExtraContent.getScope())) {
            logger.warn(
                    "compare extraContent scope different,globalExtraContent={},wsExtraContent={}",
                    globalExtraContent.toBSONObj(), wsExtraContent.toBSONObj());
            return true;
        }
        // 比较 quickStart 是否一致
        if ((globalExtraContent.isQuickStart() && !wsExtraContent.isQuickStart())
                || (!globalExtraContent.isQuickStart() && wsExtraContent.isQuickStart())) {
            logger.warn(
                    "compare extraContent quickStart different,globalExtraContent={},wsExtraContent={}",
                    globalExtraContent.toBSONObj(), wsExtraContent.toBSONObj());
            return true;
        }
        // 比较 recycleSpace 是否一致
        if ((globalExtraContent.isRecycleSpace() && !wsExtraContent.isRecycleSpace())
                || (!globalExtraContent.isRecycleSpace() && wsExtraContent.isRecycleSpace())) {
            logger.warn(
                    "compare extraContent quickStart different,globalExtraContent={},wsExtraContent={}",
                    globalExtraContent.toBSONObj(), wsExtraContent.toBSONObj());
            return true;
        }
        return false;
    }

    private boolean isDifferentMatcher(BSONObject globalMatcher, BSONObject wsMatcher) {
        if (globalMatcher == null && wsMatcher != null) {
            logger.warn("compare matcher different,globalMatcher=null,wsMatcher={}", wsMatcher);
            return true;
        }
        if (globalMatcher != null && wsMatcher == null) {
            logger.warn("compare matcher different,globalMatcher={},wsMatcher=null", globalMatcher);
            return true;
        }
        if (globalMatcher == null) {
            return false;
        }
        if (isDifferentString(globalMatcher.toString(), wsMatcher.toString())) {
            logger.warn("compare matcher different,globalMatcher={},wsMatcher={}", globalMatcher,
                    wsMatcher);
            return true;
        }
        return false;
    }

    private boolean isSameCleanTriggers(ScmCleanTriggers global, ScmCleanTriggers ws) {
        if (global == null && ws != null) {
            logger.warn(
                    "compare clean triggers different,globalCleanTriggers=null,wsCleanTriggers={}",
                    ws.toBSONObj());
            return false;
        }
        if (global != null && ws == null) {
            logger.warn(
                    "compare clean triggers different,globalCleanTriggers={},wsCleanTriggers=null",
                    global.toBSONObj());
            return false;
        }
        if (global == null) {
            return true;
        }
        // 比较mode信息是否一致
        if (isDifferentMode(global.getMode(), ws.getMode())) {
            logger.warn("compare clean triggers mode different,globalMode={},wsMode={}",
                    global.getMode(), ws.getMode());
            return false;
        }
        // 比较rule信息是否一致
        if (isDifferentRule(global.getRule(), ws.getRule())) {
            logger.warn("compare clean triggers rule different,globalRule={},wsRule={}",
                    global.getRule(), ws.getRule());
            return false;
        }
        // 比较maxExecTime信息是否一致
        if (isDifferentMaxExecTime(global.getMaxExecTime(), ws.getMaxExecTime())) {
            logger.warn(
                    "compare clean triggers maxExecTime different,globalMaxExecTime={},wsMaxExecTime={}",
                    global.getMaxExecTime(), ws.getMaxExecTime());
            return false;
        }
        // 比较triggerList信息是否一致
        if (!isSameCleanTriggerList(global.getTriggerList(), ws.getTriggerList())) {
            logger.warn(
                    "compare clean triggers triggerList different,globalTriggerList={},wsTriggerList={}",
                    global.getTriggerList(), ws.getTriggerList());
            return false;
        }
        return true;
    }

    private boolean isSameTransitionTriggers(ScmTransitionTriggers global,
            ScmTransitionTriggers ws) {
        // 比较mode信息是否一致
        if (isDifferentMode(global.getMode(), ws.getMode())) {
            logger.warn("compare transition triggers mode different,globalMode={},wsMode={}",
                    global.getMode(), ws.getMode());
            return false;
        }
        // 比较rule信息是否一致
        if (isDifferentRule(global.getRule(), ws.getRule())) {
            logger.warn("compare transition triggers rule different,globalRule={},wsRule={}",
                    global.getRule(), ws.getRule());
            return false;
        }
        // 比较maxExecTime信息是否一致
        if (isDifferentMaxExecTime(global.getMaxExecTime(), ws.getMaxExecTime())) {
            logger.warn(
                    "compare transition triggers maxExecTime different,globalMaxExecTime={},wsMaxExecTime={}",
                    global.getMaxExecTime(), ws.getMaxExecTime());
            return false;
        }

        // 比较triggerList信息是否一致
        return isSameTransitionTriggerList(global.getTriggerList(), ws.getTriggerList());
    }

    private boolean isSameCleanTriggerList(List<ScmCleanTriggers.Trigger> globalTriggerList,
            List<ScmCleanTriggers.Trigger> wsTriggerList) {
        if (globalTriggerList == null && wsTriggerList != null) {
            return false;
        }
        if (globalTriggerList != null && wsTriggerList == null) {
            return false;
        }
        if (globalTriggerList == null) {
            return true;
        }
        if (globalTriggerList.size() != wsTriggerList.size()) {
            return false;
        }

        for (int i = 0; i < globalTriggerList.size(); i++) {
            ScmCleanTriggers.Trigger globalTrigger = globalTriggerList.get(i);
            ScmCleanTriggers.Trigger wsTrigger = wsTriggerList.get(i);

            String globalMode = globalTrigger.getMode();
            String wsMode = wsTrigger.getMode();
            if (isDifferentMode(globalMode, wsMode)) {
                logger.warn("compare clean trigger mode different,globalMode={},wsMode={}",
                        globalMode, wsMode);
                return false;
            }

            String globalLastAccessTime = globalTrigger.getLastAccessTime();
            String wsLastAccessTime = wsTrigger.getLastAccessTime();
            if (isDifferentTime(globalLastAccessTime, wsLastAccessTime)) {
                logger.warn(
                        "compare clean trigger lastAccessTime different,globalLastAccessTime={},wsLastAccessTime={}",
                        globalLastAccessTime, wsLastAccessTime);
                return false;
            }

            String globalTransitionTime = globalTrigger.getTransitionTime();
            String wsTransitionTime = wsTrigger.getTransitionTime();
            if (isDifferentTime(globalTransitionTime, wsTransitionTime)) {
                logger.warn(
                        "compare clean trigger transitionTime different,globalTransitionTime={},wsTransitionTime={}",
                        globalTransitionTime, wsTransitionTime);
                return false;
            }
        }
        return true;
    }

    private boolean isSameTransitionTriggerList(
            List<ScmTransitionTriggers.Trigger> globalTriggerList,
            List<ScmTransitionTriggers.Trigger> wsTriggerList) {
        if (globalTriggerList == null && wsTriggerList != null) {
            return false;
        }
        if (globalTriggerList != null && wsTriggerList == null) {
            return false;
        }
        if (globalTriggerList == null) {
            return true;
        }
        if (globalTriggerList.size() != wsTriggerList.size()) {
            return false;
        }

        for (int i = 0; i < globalTriggerList.size(); i++) {
            ScmTransitionTriggers.Trigger globalTrigger = globalTriggerList.get(i);
            ScmTransitionTriggers.Trigger wsTrigger = wsTriggerList.get(i);

            String globalMode = globalTrigger.getMode();
            String wsMode = wsTrigger.getMode();
            if (isDifferentMode(globalMode, wsMode)) {
                logger.warn("compare transition trigger mode different,globalMode={},wsMode={}",
                        globalMode, wsMode);
                return false;
            }

            String globalLastAccessTime = globalTrigger.getLastAccessTime();
            String wsLastAccessTime = wsTrigger.getLastAccessTime();
            if (isDifferentTime(globalLastAccessTime, wsLastAccessTime)) {
                logger.warn(
                        "compare transition trigger lastAccessTime different,globalLastAccessTime={},wsLastAccessTime={}",
                        globalLastAccessTime, wsLastAccessTime);
                return false;
            }

            String globalCreateTime = globalTrigger.getCreateTime();
            String wsCreateTime = wsTrigger.getCreateTime();
            if (isDifferentTime(globalCreateTime, wsCreateTime)) {
                logger.warn(
                        "compare transition trigger createTime different,globalCreateTime={},wsCreateTime={}",
                        globalCreateTime, wsCreateTime);
                return false;
            }

            String globalBuildTime = globalTrigger.getBuildTime();
            String wsBuildTime = wsTrigger.getBuildTime();
            if (isDifferentTime(globalBuildTime, wsBuildTime)) {
                logger.warn(
                        "compare transition trigger buildTime different,globalBuildTime={},wsBuildTime={}",
                        globalBuildTime, wsBuildTime);
                return false;
            }
        }
        return true;
    }

    private boolean isDifferentTime(String globalTime, String wsTime) {
        return isDifferentString(globalTime, wsTime);
    }

    private boolean isDifferentMaxExecTime(long globalMaxExecTime, long wsMaxExecTime) {
        return globalMaxExecTime != wsMaxExecTime;
    }

    private boolean isDifferentRule(String globalRule, String wsRule) {
        return isDifferentString(globalRule, wsRule);
    }

    private boolean isDifferentMode(String globalMode, String wsMode) {
        return isDifferentString(globalMode, wsMode);
    }

    private boolean isDifferentStageTag(String global, String ws) {
        return isDifferentString(global, ws);
    }

    private boolean isDifferentString(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return false;
        }
        if (str1 == null) {
            return true;
        }
        if (str2 == null) {
            return true;
        }
        return !str1.equals(str2);
    }

    private void removeSchedule(BasicBSONList scheduleIds) throws Exception {
        for (Object scheduleId : scheduleIds) {
            ScheduleMgrWrapper.getInstance().deleteSchedule((String) scheduleId, true);
        }
    }

    private void removeScheduleSilence(BasicBSONList scheduleIds) {
        for (Object scheduleId : scheduleIds) {
            try {
                ScheduleMgrWrapper.getInstance().deleteSchedule((String) scheduleId, true);
            }
            catch (Exception e) {
                logger.error("delete schedule failed,scheduleId={}", scheduleId, e);
            }
        }
    }

    private ScmLock lockGlobal() throws ScmLockException {
        String[] lockPath = { GLOBAL_LIFE_CYCLE };
        return scmLockManager.acquiresLock(new ScmLockPath(lockPath));
    }
}
