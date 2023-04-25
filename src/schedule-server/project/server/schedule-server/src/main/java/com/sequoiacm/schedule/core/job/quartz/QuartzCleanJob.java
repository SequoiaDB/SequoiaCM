package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.TaskEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.Date;

public class QuartzCleanJob extends QuartzContentserverJob {

    @Override
    protected TaskEntity createTaskEntity(FileServerEntity runTaskServer, ScheduleJobInfo info)
            throws ScheduleException {
        CleanJobInfo cInfo = (CleanJobInfo) info;
        Date d = new Date();
        String taskId = ScmIdGenerator.TaskId.get();
        BSONObject taskCondition = createTaskContent(cInfo);
        BSONObject taskOption = createTaskOption(cInfo);
        return QuartzScheduleTools.createTask(ScheduleDefine.TaskType.SCM_TASK_CLEAN_FILE, taskId,
                taskCondition, runTaskServer.getId(), cInfo.getCheckSiteId(), d.getTime(),
                info.getWorkspace(), info.getId(), cInfo.getScope(), cInfo.getMaxExecTime(),
                taskOption, null);
    }

    @Override
    protected FileServerEntity getRunTaskServer(ScheduleJobInfo info) {
        CleanJobInfo cInfo = (CleanJobInfo) info;
        return ScheduleServer.getInstance().getRandomServer(cInfo.getSiteId(),
                info.getPreferredRegion(), info.getPreferredZone());
    }

    private BSONObject createTaskContent(CleanJobInfo cInfo) throws ScheduleException {
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

        BasicBSONList array = new BasicBSONList();
        array.add(siteCondition);

        BSONObject extraObj = cInfo.getExtraCondtion();
        if (null != extraObj && !extraObj.isEmpty()) {
            array.add(cInfo.getExtraCondtion());
        }

        BSONObject cleanTriggersObj = cInfo.getCleanTriggers();
        if (null != cleanTriggersObj && !cleanTriggersObj.isEmpty()) {
            array.add(ScheduleCommonTools.jointTriggerCondition(
                    ScheduleDefine.ScheduleType.CLEAN_FILE, cleanTriggersObj, cInfo.getSiteId(),
                    cInfo.getCheckSiteId(), d));
        }

        // -1 表示没有配置 existenceTime
        if (cInfo.getExistenceDays() != -1) {
            array.add(ScheduleCommonTools.joinCreateTimeCondition(d, cInfo.getExistenceDays()));
        }
        return new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_AND, array);
    }

    private BSONObject createTaskOption(CleanJobInfo cInfo) {
        BSONObject option = new BasicBSONObject();
        option.put(FieldName.Schedule.FIELD_QUICK_START, cInfo.isQuickStart());
        option.put(FieldName.Schedule.FIELD_IS_RECYCLE_SPACE, cInfo.isRecycleSpace());
        option.put(FieldName.Schedule.FIELD_DATA_CHECK_LEVEL, cInfo.getDataCheckLevel());
        return option;
    }
}
