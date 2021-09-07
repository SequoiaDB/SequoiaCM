package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmScheduleDao;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmScheduleBasicInfo;
import com.sequoiacm.om.omserver.module.OmScheduleInfo;
import com.sequoiacm.om.omserver.module.OmTaskBasicInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScmScheduleDaoImpl implements ScmScheduleDao {
    private ScmOmSession session;

    public ScmScheduleDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public long getScheduleCount(BSONObject condition) throws ScmInternalException {
        try {
            return ScmSystem.Schedule.count(session.getConnection(), condition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get schedule count, " + e.getMessage(), e);
        }
    }

    @Override
    public List<OmScheduleBasicInfo> getScheduleList(BSONObject condition, BSONObject orderBy,
            long skip, long limit) throws ScmInternalException {
        ScmCursor<ScmScheduleBasicInfo> scheduleCur = null;
        ScmSession conn = session.getConnection();
        try {
            if (null == orderBy) {
                orderBy = ScmQueryBuilder.start(ScmAttributeName.Schedule.CREATE_TIME).is(-1).get();
            }
            List<OmScheduleBasicInfo> scheduleList = new ArrayList<>();
            scheduleCur = ScmSystem.Schedule.list(conn, condition, orderBy, skip, limit);
            while (scheduleCur.hasNext()) {
                ScmScheduleBasicInfo scmScheduleBasicInfo = scheduleCur.getNext();
                OmScheduleBasicInfo omScheduleBasicInfo = transformToScheduleBasicInfo(
                        scmScheduleBasicInfo);
                scheduleList.add(omScheduleBasicInfo);
            }
            return scheduleList;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get schedule list, " + e.getMessage(), e);
        }
        finally {
            if (null != scheduleCur) {
                scheduleCur.close();
            }
        }

    }

    @Override
    public void createSchedule(OmScheduleInfo scheduleInfo)
            throws ScmInternalException, ScmOmServerException {
        try {
            ScmSession conn = session.getConnection();
            ScmScheduleContent scheduleContent = null;
            ScheduleType scheduleType = ScheduleType.getType(scheduleInfo.getType());
            ScmType.ScopeType scopeType = ScmType.ScopeType
                    .getScopeType(scheduleInfo.getScopeType());
            if (scheduleType == ScheduleType.CLEAN_FILE) {
                scheduleContent = new ScmScheduleCleanFileContent(scheduleInfo.getSourceSite(),
                        scheduleInfo.getMaxStayTime(), scheduleInfo.getCondition(), scopeType,
                        scheduleInfo.getMaxExecTime());
            }
            else if (scheduleType == ScheduleType.COPY_FILE) {
                scheduleContent = new ScmScheduleCopyFileContent(scheduleInfo.getSourceSite(),
                        scheduleInfo.getTargetSite(), scheduleInfo.getMaxStayTime(),
                        scheduleInfo.getCondition(), scopeType, scheduleInfo.getMaxExecTime());
            }
            else {
                throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                        "invalid scope type:scope type=" + scheduleInfo.getType());
            }
            ScmScheduleBuilder scheduleBuilder = ScmSystem.Schedule.scheduleBuilder(conn);
            scheduleBuilder.workspace(scheduleInfo.getWorkspace()).type(scheduleType)
                    .name(scheduleInfo.getName()).content(scheduleContent)
                    .cron(scheduleInfo.getCron()).description(scheduleInfo.getDescription())
                    .preferredRegion(scheduleInfo.getPreferredRegion())
                    .preferredZone(scheduleInfo.getPreferredZone())
                    .enable(scheduleInfo.getEnable());
            scheduleBuilder.build();
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to create schedule, " + e.getMessage(), e);
        }
    }

    @Override
    public OmScheduleInfo getScheduleDetail(String scheduleId) throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            ScmId scmScheduleId = new ScmId(scheduleId);
            ScmSchedule scmSchedule = ScmSystem.Schedule.get(conn, scmScheduleId);
            return transformToOmScheduleInfo(scmSchedule);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get schedule detail, " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSchedule(String scheduleId) throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            ScmSystem.Schedule.delete(conn, new ScmId(scheduleId));
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to delete schedule, " + e.getMessage(), e);
        }
    }

    @Override
    public void updateSchedule(OmScheduleInfo newInfo)
            throws ScmInternalException, ScmOmServerException {
        ScmSession conn = session.getConnection();
        try {
            ScmSchedule oldInfo = ScmSystem.Schedule.get(conn, new ScmId(newInfo.getScheduleId()));
            ScmScheduleContent content = null;
            String name = newInfo.getName();
            String cron = newInfo.getCron();
            String desc = newInfo.getDescription();
            Boolean enable = newInfo.getEnable();
            String workspace = newInfo.getWorkspace();
            String preferredRegion = newInfo.getPreferredRegion();
            String preferredZone = newInfo.getPreferredZone();
            String type = getValueOrDefault(newInfo.getType(), oldInfo.getType().getName());
            if (ScheduleType.getType(type) == ScheduleType.CLEAN_FILE) {
                content = createCleanFileContentFromOld(newInfo, oldInfo.getContent());
            }
            else if (ScheduleType.getType(type) == ScheduleType.COPY_FILE) {
                content = createCopyFileContentFromOld(newInfo, oldInfo.getContent());
            }
            else {
                throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                        "invalid scope type:scope type=" + type);
            }
            oldInfo.updateSchedule(name, cron, workspace, desc, ScheduleType.getType(type), enable,
                    preferredRegion, preferredZone, content);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to update schedule, " + e.getMessage(), e);
        }
    }

    @Override
    public List<OmTaskBasicInfo> getTasks(String scheduleId, BSONObject filter, BSONObject orderBy,
            long skip, long limit) throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            if (null == orderBy) {
                orderBy = ScmQueryBuilder.start(ScmAttributeName.Task.START_TIME).is(-1).get();
            }
            ScmSchedule scmSchedule = ScmSystem.Schedule.get(conn, new ScmId(scheduleId));
            List<ScmTask> tasks = scmSchedule.getTasks(filter, orderBy, skip, limit);
            return transformToOmTaskList(tasks);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get task list, " + e.getMessage(), e);
        }
    }

    private List<OmTaskBasicInfo> transformToOmTaskList(List<ScmTask> tasks) {
        if (tasks == null || tasks.size() <= 0) {
            return Collections.emptyList();
        }
        List<OmTaskBasicInfo> omTaskList = new ArrayList<>(tasks.size());
        for (ScmTask scmTask : tasks) {
            OmTaskBasicInfo omTask = new OmTaskBasicInfo();
            omTask.setTaskId(scmTask.getId().get());
            omTask.setActualCount(scmTask.getActualCount());
            omTask.setFailCount(scmTask.getFailCount());
            omTask.setSuccessCount(scmTask.getSuccessCount());
            omTask.setProgress(scmTask.getProgress());
            omTask.setStatus(scmTask.getRunningFlag());
            omTask.setStartTime(scmTask.getStartTime());
            omTask.setStopTime(scmTask.getStopTime());
            omTaskList.add(omTask);
        }
        return omTaskList;
    }

    private ScmScheduleContent createCleanFileContentFromOld(OmScheduleInfo newInfo,
            ScmScheduleContent oldContent) throws ScmException {
        String site = null;
        BSONObject condition = null;
        String maxStayTime = null;
        Long maxExecTime = null;
        Integer scopeType = null;
        if (oldContent instanceof ScmScheduleCleanFileContent) {
            ScmScheduleCleanFileContent old = (ScmScheduleCleanFileContent) oldContent;
            site = getValueOrDefault(newInfo.getSourceSite(), old.getSiteName());
            condition = getValueOrDefault(newInfo.getCondition(), old.getExtraCondition());
            maxStayTime = getValueOrDefault(newInfo.getMaxStayTime(), old.getMaxStayTime());
            maxExecTime = getValueOrDefault(newInfo.getMaxExecTime(), old.getMaxExecTime());
            scopeType = getValueOrDefault(newInfo.getScopeType(), old.getScope().getScope());
        }
        else if (oldContent instanceof ScmScheduleCopyFileContent) {
            ScmScheduleCopyFileContent old = (ScmScheduleCopyFileContent) oldContent;
            site = getValueOrDefault(newInfo.getSourceSite(), old.getSourceSiteName());
            condition = getValueOrDefault(newInfo.getCondition(), old.getExtraCondition());
            maxStayTime = getValueOrDefault(newInfo.getMaxStayTime(), old.getMaxStayTime());
            maxExecTime = getValueOrDefault(newInfo.getMaxExecTime(), old.getMaxExecTime());
            scopeType = getValueOrDefault(newInfo.getScopeType(), old.getScope().getScope());
        }
        return new ScmScheduleCleanFileContent(site, maxStayTime, condition,
                ScmType.ScopeType.getScopeType(scopeType), maxExecTime);
    }

    private ScmScheduleContent createCopyFileContentFromOld(OmScheduleInfo newInfo,
            ScmScheduleContent oldContent) throws ScmException {
        String sourceSite = null;
        String targetSite = null;
        BSONObject condition = null;
        String maxStayTime = null;
        Long maxExecTime = null;
        Integer scopeType = null;
        if (oldContent instanceof ScmScheduleCleanFileContent) {
            ScmScheduleCleanFileContent old = (ScmScheduleCleanFileContent) oldContent;
            sourceSite = getValueOrDefault(newInfo.getSourceSite(), old.getSiteName());
            targetSite = newInfo.getTargetSite();
            condition = getValueOrDefault(newInfo.getCondition(), old.getExtraCondition());
            maxStayTime = getValueOrDefault(newInfo.getMaxStayTime(), old.getMaxStayTime());
            maxExecTime = getValueOrDefault(newInfo.getMaxExecTime(), old.getMaxExecTime());
            scopeType = getValueOrDefault(newInfo.getScopeType(), old.getScope().getScope());
        }
        else if (oldContent instanceof ScmScheduleCopyFileContent) {
            ScmScheduleCopyFileContent old = (ScmScheduleCopyFileContent) oldContent;
            sourceSite = getValueOrDefault(newInfo.getSourceSite(), old.getSourceSiteName());
            targetSite = getValueOrDefault(newInfo.getTargetSite(), old.getTargetSiteName());
            condition = getValueOrDefault(newInfo.getCondition(), old.getExtraCondition());
            maxStayTime = getValueOrDefault(newInfo.getMaxStayTime(), old.getMaxStayTime());
            maxExecTime = getValueOrDefault(newInfo.getMaxExecTime(), old.getMaxExecTime());
            scopeType = getValueOrDefault(newInfo.getScopeType(), old.getScope().getScope());
        }
        return new ScmScheduleCopyFileContent(sourceSite, targetSite, maxStayTime, condition,
                ScmType.ScopeType.getScopeType(scopeType), maxExecTime);
    }

    @SuppressWarnings("unchecked")
    private <T> T getValueOrDefault(Object value, Object defaultValue) {
        return (T) (value != null ? value : defaultValue);
    }

    private OmScheduleInfo transformToOmScheduleInfo(ScmSchedule scmSchedule) {
        ScmScheduleContent content = scmSchedule.getContent();
        ScheduleType scheduleType = scmSchedule.getType();
        OmScheduleInfo omScheduleInfo = new OmScheduleInfo();
        omScheduleInfo.setScheduleId(scmSchedule.getId().get());
        omScheduleInfo.setEnable(scmSchedule.isEnable());
        omScheduleInfo.setCron(scmSchedule.getCron());
        omScheduleInfo.setName(scmSchedule.getName());
        omScheduleInfo.setWorkspace(scmSchedule.getWorkspace());
        omScheduleInfo.setDescription(scmSchedule.getDesc());
        omScheduleInfo.setCreateTime(scmSchedule.getCreateDate());
        omScheduleInfo.setCreateUser(scmSchedule.getCreaateUser());
        omScheduleInfo.setType(scheduleType.getName());
        omScheduleInfo.setPreferredRegion(scmSchedule.getPreferredRegion());
        omScheduleInfo.setPreferredZone(scmSchedule.getPreferredZone());
        if (content instanceof ScmScheduleCleanFileContent) {
            ScmScheduleCleanFileContent cleanFileContent = (ScmScheduleCleanFileContent) content;
            omScheduleInfo.setCondition(cleanFileContent.getExtraCondition());
            omScheduleInfo.setSourceSite(cleanFileContent.getSiteName());
            omScheduleInfo.setMaxStayTime(cleanFileContent.getMaxStayTime());
            omScheduleInfo.setMaxExecTime(cleanFileContent.getMaxExecTime());
            omScheduleInfo.setScopeType(cleanFileContent.getScope().getScope());
        }
        else if (content instanceof ScmScheduleCopyFileContent) {
            ScmScheduleCopyFileContent copyFileContent = (ScmScheduleCopyFileContent) content;
            omScheduleInfo.setCondition(copyFileContent.getExtraCondition());
            omScheduleInfo.setSourceSite(copyFileContent.getSourceSiteName());
            omScheduleInfo.setTargetSite(copyFileContent.getTargetSiteName());
            omScheduleInfo.setMaxStayTime(copyFileContent.getMaxStayTime());
            omScheduleInfo.setMaxExecTime(copyFileContent.getMaxExecTime());
            omScheduleInfo.setScopeType(copyFileContent.getScope().getScope());
        }
        return omScheduleInfo;
    }

    private OmScheduleBasicInfo transformToScheduleBasicInfo(
            ScmScheduleBasicInfo scmScheduleBasicInfo) {
        OmScheduleBasicInfo omScheduleBasicInfo = new OmScheduleBasicInfo();
        omScheduleBasicInfo.setScheduleId(scmScheduleBasicInfo.getId().get());
        omScheduleBasicInfo.setWorkspace(scmScheduleBasicInfo.getWorkspace());
        omScheduleBasicInfo.setDescription(scmScheduleBasicInfo.getDesc());
        omScheduleBasicInfo.setEnable(scmScheduleBasicInfo.isEnable());
        omScheduleBasicInfo.setName(scmScheduleBasicInfo.getName());
        omScheduleBasicInfo.setType(scmScheduleBasicInfo.getType().getName());
        return omScheduleBasicInfo;
    }
}
