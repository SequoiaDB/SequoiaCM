package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleDefine;
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
    protected TaskEntity createTaskEntity(FileServerEntity runTaskServer, ScheduleJobInfo info) {
        CleanJobInfo cInfo = (CleanJobInfo) info;
        Date d = new Date();
        String taskId = ScmIdGenerator.TaskId.get();
        BSONObject taskCondition = createTaskContent(cInfo);
        return QuartzScheduleTools.createTask(ScheduleDefine.TaskType.SCM_TASK_CLEAN_FILE, taskId,
                taskCondition, runTaskServer.getId(), null, d.getTime(), info.getWorkspace(),
                info.getId(), cInfo.getScope(), cInfo.getMaxExecTime());
    }

    @Override
    protected FileServerEntity getRunTaskServer(ScheduleJobInfo info) {
        CleanJobInfo cInfo = (CleanJobInfo) info;
        return ScheduleServer.getInstance().getRandomServer(cInfo.getSiteId(),
                info.getPreferredRegion(), info.getPreferredZone());
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
        return new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_AND, array);
    }

}
