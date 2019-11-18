package com.sequoiacm.schedule.core.job.quartz;

import java.util.Date;
import java.util.concurrent.locks.Lock;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.TaskEntity;
import com.sequoiacm.schedule.exception.ScheduleException;
import com.sequoiacm.schedule.remote.ScheduleClient;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;

public class QuartzCleanJob extends QuartzScheduleJob {
    private static final Logger logger = LoggerFactory.getLogger(QuartzCleanJob.class);

    @Override
    public void execute(ScheduleJobInfo info) throws ScheduleException {
        CleanJobInfo cInfo = (CleanJobInfo) info;
        logger.debug(
                "id={},type={},workspace={},days={},site={},siteId={},extra={},cron={},scope={},maxExecTime={}",
                cInfo.getId(), cInfo.getType(), cInfo.getWorkspace(), cInfo.getDays(),
                cInfo.getSiteName(), cInfo.getSiteId(), cInfo.getExtraCondtion(), cInfo.getCron(),
                cInfo.getScope(), cInfo.getMaxExecTime());

        FileServerEntity s = ScheduleServer.getInstance().getRandomServer(cInfo.getSiteId());
        if (null == s) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "server is not exist in site:site=" + cInfo.getSiteName() + ",site_id="
                            + cInfo.getSiteId());
        }

        BSONObject taskCondition = createTaskContent(cInfo);

        Date d = new Date();
        String taskId = ScmIdGenerator.TaskId.get();

        TaskEntity task = QuartzScheduleTools.createTask(
                ScheduleDefine.TaskType.SCM_TASK_CLEAN_FILE, taskId, taskCondition, s.getId(), null,
                d.getTime(), info.getWorkspace(), info.getId(), cInfo.getScope(),
                cInfo.getMaxExecTime());

        BSONObject condition = QuartzScheduleTools.createDuplicateTaskMatcher(
                ScheduleDefine.TaskType.SCM_TASK_CLEAN_FILE, info.getWorkspace());
        Lock lock = null;
        try {
            lock = QuartzScheduleTools.getDuplicateTaskLock();
            lock.lock();
            if (ScheduleServer.getInstance().isTaskExist(condition)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "task is conflicted:task=" + task);
            }
            ScheduleServer.getInstance().insertTask(task);
        }
        catch (ScheduleException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "insert task fialed:task=" + task, e);
        }
        finally {
            if (null != lock) {
                lock.unlock();
            }
        }

        try {
            String targetUrl = ScheduleCommonTools.createContentServerInternalUrl(s.getHostName(),
                    s.getPort());
            ScheduleClientFactory clientFactory = ScheduleMgrWrapper.getInstance()
                    .getClientFactory();
            ScheduleClient client = clientFactory.getFeignClientByNodeUrl(targetUrl);
            client.notifyTask(taskId, RestCommonDefine.RestParam.VALUE_NOTIFY_TYPE_START);
        }
        catch (Exception e) {
            logger.error("notify task failed", e);
            QuartzScheduleTools.deleteTask(taskId);
        }
    }

    private BSONObject createTaskContent(CleanJobInfo cInfo) {
        Date d = new Date();
        BSONObject ltTimes = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_LT,
                d.getTime() - cInfo.getDays() * 24L * 3600L * 1000L);
        BSONObject createTimeLtTimes = new BasicBSONObject(
                FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, ltTimes);
        createTimeLtTimes.put(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_ID, cInfo.getSiteId());

        BSONObject elemMatch = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_ELEMMATCH,
                createTimeLtTimes);
        BSONObject siteCondition = new BasicBSONObject(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST,
                elemMatch);

        BSONObject array = new BasicBSONList();
        array.put("0", siteCondition);

        BSONObject extraObj = cInfo.getExtraCondtion();
        if (null != extraObj && !extraObj.isEmpty()) {
            array.put("1", cInfo.getExtraCondtion());
        }

        BSONObject taskCondition = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_AND, array);

        return taskCondition;
    }

}
