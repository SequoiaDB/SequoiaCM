package com.sequoiacm.schedule.client;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.schedule.client.feign.ScheduleFeignClient;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.ScheduleExceptionConverter;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;

@Component
public class ScheduleClient {
    private ScheduleFeignClient feign;

    @Autowired
    public ScheduleClient(ScmFeignClient feignClient) {
        feign = feignClient.builder().exceptionConverter(new ScheduleExceptionConverter())
                .serviceTarget(ScheduleFeignClient.class, "schedule-server");
    }

    public ScheduleFullEntity createSchedule(ScheduleUserEntity sch) throws ScheduleException {
        String schJson = ScheduleEntityTranslator.UserInfo.toJSONString(sch);
        return feign.createSchedule(schJson);
    }

    public void deleteSchedule(String schId, boolean stopWorker) throws ScheduleException {
        feign.deleteSchedule(schId, stopWorker);
    }

    public List<ScheduleFullEntity> listSchedule(BSONObject condition) throws ScheduleException {
        return feign.listSchedule(condition.toString());
    }

    public void reportStatus(InternalSchStatus status) throws ScheduleException {
        feign.reportInternalSchStatus(status.getSchId(), status.getWorkerNode(),
                status.getStartTime(), status.getStatus(), status.isFinish());
    }

    public InternalSchStatus getInternalSchLatestStatus(String schName) throws ScheduleException {
        return feign.getInternalSchLatestStatus(schName);
    }

    public ScheduleFullEntity getInternalSchduleByName(String name) throws ScheduleException {
        BasicBSONObject condition = new BasicBSONObject(FieldName.Schedule.FIELD_NAME, name);
        condition.put(FieldName.Schedule.FIELD_TYPE, ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE);
        List<ScheduleFullEntity> schs = listSchedule(condition);
        if (schs.size() == 0) {
            return null;
        }
        return schs.get(0);
    }
}
