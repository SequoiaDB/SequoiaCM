package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.TaskEntity;
import com.sequoiacm.schedule.remote.ScheduleClient;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class QuartzContentserverJob extends QuartzScheduleJob {
    private static final Logger logger = LoggerFactory.getLogger(QuartzContentserverJob.class);

    @Override
    public void execute(ScheduleJobInfo info, JobExecutionContext context)
            throws ScheduleException {
        logger.debug("schedule trigger: {}", info);

        // 检查本调度一个最新的Task（状态为 Running or Init），通过 notify 请求确认任务执行节点的运行状态，
        // 若正在运行则忽略本次调度的触发，否则正常触发新建一个 Task
        TaskEntity initOrRunningTask = getScheduleInitOrRunningTask(info.getId());
        if (initOrRunningTask != null) {
            FileServerEntity server = ScheduleServer.getInstance()
                    .getServerById(initOrRunningTask.getServerId());
            if (server != null) {
                try {
                    notifyTask(server, initOrRunningTask.getId());
                    logger.info(
                            "the previous task is running, ignore this trigger: runningTask: {}",
                            initOrRunningTask);
                    return;
                }
                catch (Exception e) {
                    logger.warn(
                            "failed to check the exist task status, rerun a new task: oldTask={}",
                            initOrRunningTask, e);
                }
            }
            else {
                logger.warn("contentserver not found, assume the previous task is not running: {}",
                        initOrRunningTask);
            }
        }

        FileServerEntity runTaskServer = getRunTaskServer(info);
        if (null == runTaskServer) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "no contentserver to run task:" + info);
        }

        TaskEntity task = createTaskEntity(runTaskServer, info);

        try {
            ScheduleServer.getInstance().insertTask(task);
            logger.debug("task created: {}", task);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "insert task failed:task=" + task, e);
        }

        try {
            notifyTask(runTaskServer, task.getId());
        }
        catch (Exception e) {
            logger.error("notify task failed, delete the task: task={}", task, e);
            QuartzScheduleTools.deleteTask(task.getId());
        }
    }

    protected abstract TaskEntity createTaskEntity(FileServerEntity runTaskServer,
            ScheduleJobInfo info) throws ScheduleException;

    protected abstract FileServerEntity getRunTaskServer(ScheduleJobInfo info)
            throws ScheduleException;

    private TaskEntity getScheduleInitOrRunningTask(String scheduleId) throws ScheduleException {
        BSONObject condition = new BasicBSONObject();
        condition.put(FieldName.Task.FIELD_SCHEDULE_ID, scheduleId);

        BasicBSONList orCondition = new BasicBSONList();
        orCondition.add(new BasicBSONObject(FieldName.Task.FIELD_RUNNING_FLAG,
                ScheduleDefine.TaskRunningFlag.SCM_TASK_RUNNING));
        orCondition.add(new BasicBSONObject(FieldName.Task.FIELD_RUNNING_FLAG,
                ScheduleDefine.TaskRunningFlag.SCM_TASK_INIT));

        condition.put("$or", orCondition);
        try {
            return ScheduleServer.getInstance().queryLatestTask(condition);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "query task fialed:scheduleId=" + scheduleId, e);
        }
    }

    private void notifyTask(FileServerEntity server, String taskId) {
        String targetUrl = ScheduleCommonTools.createContentServerInternalUrl(server.getHostName(),
                server.getPort());
        ScheduleClientFactory clientFactory = ScheduleMgrWrapper.getInstance().getClientFactory();
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(targetUrl);
        client.notifyTask(taskId, RestCommonDefine.RestParam.VALUE_NOTIFY_TYPE_START);
    }
}
