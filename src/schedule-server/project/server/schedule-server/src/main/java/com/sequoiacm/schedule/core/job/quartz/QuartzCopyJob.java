package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.CopyJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.TaskEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.Date;

public class QuartzCopyJob extends QuartzContentserverJob {

    @Override
    protected TaskEntity createTaskEntity(FileServerEntity runTaskServer, ScheduleJobInfo info)
            throws ScheduleException {
        CopyJobInfo cInfo = (CopyJobInfo) info;
        BSONObject taskCondition = createTaskContent(cInfo);

        Date d = new Date();
        String taskId = ScmIdGenerator.TaskId.get();
        BSONObject taskOption = createTaskOption(cInfo);
        return QuartzScheduleTools.createTask(ScheduleDefine.TaskType.SCM_TASK_COPY_FILE, taskId,
                taskCondition, runTaskServer.getId(), cInfo.getTargetSiteId(), d.getTime(),
                info.getWorkspace(), info.getId(), cInfo.getScope(), cInfo.getMaxExecTime(),
                taskOption, null);
    }

    @Override
    protected FileServerEntity getRunTaskServer(ScheduleJobInfo info) {
        CopyJobInfo cInfo = (CopyJobInfo) info;
        return ScheduleServer.getInstance().getRandomServer(cInfo.getSourceSiteId(),
                info.getPreferredRegion(), info.getPreferredZone());
    }

    private BSONObject createTaskContent(CopyJobInfo cInfo) throws ScheduleException {
        Date d = new Date();
        BSONObject ltTimes = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_LT,
                d.getTime() - cInfo.getDays() * 24L * 3600L * 1000L);
        BSONObject createTimeLtTimes = new BasicBSONObject(
                FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, ltTimes);
        createTimeLtTimes.put(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_ID,
                cInfo.getSourceSiteId());

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

        BSONObject transitionTriggersObj = cInfo.getTransitionTriggers();
        if (null != transitionTriggersObj && !transitionTriggersObj.isEmpty()) {
            array.add(ScheduleCommonTools.jointTriggerCondition(
                    ScheduleDefine.ScheduleType.COPY_FILE, transitionTriggersObj,
                    cInfo.getSourceSiteId(),
                    cInfo.getTargetSiteId(), d));
        }

        return new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_AND, array);
    }

    private BSONObject createTaskOption(CopyJobInfo cInfo) {
        BSONObject option = new BasicBSONObject();
        option.put(FieldName.Schedule.FIELD_QUICK_START, cInfo.isQuickStart());
        option.put(FieldName.Schedule.FIELD_DATA_CHECK_LEVEL, cInfo.getDataCheckLevel());
        return option;
    }
}
