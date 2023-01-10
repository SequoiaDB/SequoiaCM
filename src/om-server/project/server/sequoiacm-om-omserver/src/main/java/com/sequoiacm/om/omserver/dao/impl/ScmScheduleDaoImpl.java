package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.common.ScheduleType;
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
import org.apache.commons.lang.StringUtils;
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
            ScmScheduleContent scheduleContent = generateScheduleContent(scheduleInfo);
            ScmScheduleBuilder scheduleBuilder = ScmSystem.Schedule.scheduleBuilder(conn);
            scheduleBuilder.workspace(scheduleInfo.getWorkspace())
                    .type(ScheduleType.getType(scheduleInfo.getType())).name(scheduleInfo.getName())
                    .content(scheduleContent).cron(scheduleInfo.getCron())
                    .description(scheduleInfo.getDescription())
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
            ScmSchedule schedule = ScmSystem.Schedule.get(conn, new ScmId(newInfo.getScheduleId()));
            String scheduleType = schedule.getType().getName();
            String newType = newInfo.getType();
            if (newType != null && !StringUtils.equals(newType, scheduleType)) {
                throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                        "update schedule type not supported");
            }
            ScmScheduleContent content = generateScheduleContent(newInfo);
            schedule.updateSchedule(newInfo.getName(), newInfo.getCron(), newInfo.getWorkspace(),
                    newInfo.getDescription(), ScheduleType.getType(scheduleType),
                    newInfo.getEnable(), newInfo.getPreferredRegion(), newInfo.getPreferredZone(),
                    content);
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
            omTask.setWorkspace(scmTask.getWorkspaceName());
            omTask.setEstimateCount(scmTask.getEstimateCount());
            omTask.setActualCount(scmTask.getActualCount());
            omTask.setFailCount(scmTask.getFailCount());
            omTask.setSuccessCount(scmTask.getSuccessCount());
            omTask.setProgress(scmTask.getProgress());
            omTask.setStatus(scmTask.getRunningFlag());
            omTask.setStartTime(scmTask.getStartTime());
            omTask.setStopTime(scmTask.getStopTime());
            omTask.setContent(scmTask.getContent());
            omTask.setExtraInfo(scmTask.getExtraInfo());
            omTaskList.add(omTask);
        }
        return omTaskList;
    }

    private ScmScheduleContent generateScheduleContent(OmScheduleInfo scheduleInfo)
            throws ScmException, ScmOmServerException {
        if (scheduleInfo.getType() == null) {
            return null;
        }
        ScheduleType scheduleType = ScheduleType.getType(scheduleInfo.getType());
        if (scheduleType == ScheduleType.CLEAN_FILE) {
            return new ScmScheduleCleanFileContent(scheduleInfo.getContent());
        }
        else if (scheduleType == ScheduleType.COPY_FILE) {
            return new ScmScheduleCopyFileContent(scheduleInfo.getContent());
        }
        else if (scheduleType == ScheduleType.MOVE_FILE) {
            return new ScmScheduleMoveFileContent(scheduleInfo.getContent());
        }
        else if (scheduleType == ScheduleType.RECYCLE_SPACE) {
            return new ScmScheduleSpaceRecyclingContent(scheduleInfo.getContent());
        }
        throw new ScmOmServerException(ScmOmServerError.INVALID_ARGUMENT,
                "invalid schedule type:schedule type=" + scheduleInfo.getType());
    }

    @SuppressWarnings("unchecked")
    private <T> T getValueOrDefault(Object value, Object defaultValue) {
        return (T) (value != null ? value : defaultValue);
    }

    private OmScheduleInfo transformToOmScheduleInfo(ScmSchedule scmSchedule) {
        ScheduleType scheduleType = scmSchedule.getType();
        OmScheduleInfo omScheduleInfo = new OmScheduleInfo();
        omScheduleInfo.setScheduleId(scmSchedule.getId().get());
        omScheduleInfo.setEnable(scmSchedule.isEnable());
        omScheduleInfo.setCron(scmSchedule.getCron());
        omScheduleInfo.setName(scmSchedule.getName());
        omScheduleInfo.setWorkspace(scmSchedule.getWorkspace());
        omScheduleInfo.setTransition(scmSchedule.getTransitionName());
        omScheduleInfo.setDescription(scmSchedule.getDesc());
        omScheduleInfo.setCreateTime(scmSchedule.getCreateDate());
        omScheduleInfo.setCreateUser(scmSchedule.getCreaateUser());
        omScheduleInfo.setType(scheduleType.getName());
        omScheduleInfo.setPreferredRegion(scmSchedule.getPreferredRegion());
        omScheduleInfo.setPreferredZone(scmSchedule.getPreferredZone());
        omScheduleInfo.setContent(scmSchedule.getContent().toBSONObject());
        return omScheduleInfo;
    }

    private OmScheduleBasicInfo transformToScheduleBasicInfo(
            ScmScheduleBasicInfo scmScheduleBasicInfo) {
        OmScheduleBasicInfo omScheduleBasicInfo = new OmScheduleBasicInfo();
        omScheduleBasicInfo.setScheduleId(scmScheduleBasicInfo.getId().get());
        omScheduleBasicInfo.setWorkspace(scmScheduleBasicInfo.getWorkspace());
        omScheduleBasicInfo.setTransition(scmScheduleBasicInfo.getTransition());
        omScheduleBasicInfo.setDescription(scmScheduleBasicInfo.getDesc());
        omScheduleBasicInfo.setEnable(scmScheduleBasicInfo.isEnable());
        omScheduleBasicInfo.setName(scmScheduleBasicInfo.getName());
        omScheduleBasicInfo.setType(scmScheduleBasicInfo.getType().getName());
        return omScheduleBasicInfo;
    }
}
