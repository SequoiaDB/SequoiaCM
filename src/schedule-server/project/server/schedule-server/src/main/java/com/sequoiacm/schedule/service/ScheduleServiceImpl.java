package com.sequoiacm.schedule.service;

import com.sequoiacm.infrastructure.common.NetUtil;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.*;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.dao.InternalSchStatusDao;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    @Autowired
    private InternalSchStatusDao dao;

    @Autowired
    private ScheduleDao schDao;

    @Override
    public ScheduleFullEntity createSchedule(String createUser, ScheduleUserEntity info)
            throws Exception {
        return ScheduleMgrWrapper.getInstance().createSchedule(createUser, info);
    }

    @Override
    public void deleteSchedule(String scheduleId, boolean stopWorker) throws Exception {
        ScheduleMgrWrapper.getInstance().deleteSchedule(scheduleId, stopWorker);
    }

    @Override
    public ScheduleFullEntity getSchedule(String scheduleId) throws Exception {
        return ScheduleMgrWrapper.getInstance().getSchedule(scheduleId);
    }

    @Override
    public ScmBSONObjectCursor listSchedule(BSONObject condition) throws Exception {
        return ScheduleMgrWrapper.getInstance().getSchedule(condition);
    }

    @Override
    public ScmBSONObjectCursor listSchedule(BSONObject condition, BSONObject orderBy, long skip,
            long limit) throws Exception {
        return ScheduleMgrWrapper.getInstance().getSchedule(condition, orderBy, skip, limit);
    }

    @Override
    public ScheduleFullEntity updateSchedule(String scheduleId, ScheduleNewUserInfo newInfo)
            throws Exception {
        return ScheduleMgrWrapper.getInstance().updateSchedule(scheduleId, newInfo);
    }

    @Override
    public InternalSchStatus getInternalSchLatestStatusByName(String scheduleName)
            throws Exception {
        // 只能排序去查最新的记录，schedule 表里的记录可能已经被删除了。
        return dao.getLatestStatusByName(scheduleName);
    }

    @Override
    public void reportInternalSchStatus(InternalSchStatus status) throws Exception {
        ScheduleFullEntity sch = schDao.queryOne(status.getSchId());
        if (sch == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKER_SHOULD_STOP,
                    "schedule not exist:" + status.getSchId());
        }

        if (!sch.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKER_SHOULD_STOP,
                    "this schedule is not internal_schedule:" + sch);
        }
        InternalScheduleInfo jobInfo = new InternalScheduleInfo(sch.getId(), sch.getName(),
                sch.getType(), sch.getWorkspace(), sch.getContent(), sch.getCron(),
                sch.getPreferredRegion(), sch.getPreferredZone());
        if (!workerNoderEquals(jobInfo.getWorkerNode(), status.getWorkerNode())
                || jobInfo.getWorkerNodeStartTime() != status.getStartTime()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKER_SHOULD_STOP,
                    "this schedule worker is change:" + sch);
        }
        status.setSchName(sch.getName());
        dao.upsertStatus(status);
    }

    @Override
    public long countSchedule(BSONObject condition) throws Exception {
        return schDao.countSchedule(condition);
    }

    public boolean workerNoderEquals(String jobInfoWorkNode, String statusWorkNode)
            throws UnknownHostException, ScheduleException {
        if (!jobInfoWorkNode.equals(statusWorkNode)) {
            String[] splitstatusWorkerNode = statusWorkNode.split(":");
            String[] splitjobInfoWorkerNode = jobInfoWorkNode.split(":");
            if (splitstatusWorkerNode.length != 2 || splitjobInfoWorkerNode.length != 2) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "the format of worknode is incorret, host names are: " + jobInfoWorkNode
                                + " and " + statusWorkNode);
            }

            if (splitjobInfoWorkerNode[1].equals(splitstatusWorkerNode[1])) {
                return NetUtil.isSameHost(splitstatusWorkerNode[0], splitjobInfoWorkerNode[0]);
            }
            return false;
        }
        return true;
    }
}
