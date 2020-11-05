package com.sequoiacm.schedule.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleNewUserInfo;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.dao.InternalSchStatusDao;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

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
                sch.getType(), sch.getWorkspace(), sch.getContent(), sch.getCron());
        if (!workerNoderEquals(jobInfo.getWorkerNode(), status.getWorkerNode())
                || jobInfo.getWorkerNodeStartTime() != status.getStartTime()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKER_SHOULD_STOP,
                    "this schedule worker is change:" + sch);
        }
        status.setSchName(sch.getName());
        dao.upsertStatus(status);
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

                InetAddress statusInetAddress = InetAddress.getByName(splitstatusWorkerNode[0]);
                String statusHostIp = statusInetAddress.getHostAddress();

                InetAddress jobInfoInetAddress = InetAddress.getByName(splitjobInfoWorkerNode[0]);
                String jobInfoHostIp = jobInfoInetAddress.getHostAddress();
                if (jobInfoHostIp.equals(statusHostIp)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}
