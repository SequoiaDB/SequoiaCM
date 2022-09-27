package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.core.job.SpaceRecyclingJobInfo;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.TaskEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.Date;

public class QuartzSpaceRecyclingJob extends QuartzContentserverJob {

    @Override
    protected TaskEntity createTaskEntity(FileServerEntity runTaskServer, ScheduleJobInfo info)
            throws ScheduleException {
        SpaceRecyclingJobInfo recyclingJobInfo = (SpaceRecyclingJobInfo) info;
        Date d = new Date();
        String taskId = ScmIdGenerator.TaskId.get();
        BSONObject taskOption = createTaskOption(recyclingJobInfo);
        return QuartzScheduleTools.createTask(ScheduleDefine.TaskType.SCM_TASK_RECYCLE_SAPCE,
                taskId, null, runTaskServer.getId(), recyclingJobInfo.getTargetSiteId(),
                d.getTime(), info.getWorkspace(), info.getId(), -1,
                recyclingJobInfo.getMaxExecTime(), taskOption, null);
    }

    private BSONObject createTaskOption(SpaceRecyclingJobInfo jobInfo) {
        BSONObject option = new BasicBSONObject();
        option.put(FieldName.Schedule.FIELD_SPACE_RECYCLING_SCOPE, jobInfo.getRecycleScope());
        return option;
    }

    @Override
    protected FileServerEntity getRunTaskServer(ScheduleJobInfo info) throws ScheduleException {
        SpaceRecyclingJobInfo recyclingJobInfo = (SpaceRecyclingJobInfo) info;
        return ScheduleServer.getInstance().getRandomServer(recyclingJobInfo.getTargetSiteId(),
                info.getPreferredRegion(), info.getPreferredZone());
    }
}
