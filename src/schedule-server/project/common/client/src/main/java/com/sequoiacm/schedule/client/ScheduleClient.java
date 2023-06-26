package com.sequoiacm.schedule.client;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private static final Logger logger = LoggerFactory.getLogger(ScheduleClient.class);
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
        try {
            return feign.getInternalSchLatestStatusV2(schName);
        }
        catch (ScheduleException e) {
            if (!e.getCode().equalsIgnoreCase(HttpStatus.NOT_FOUND.getReasonPhrase())) {
                throw e;
            }
            logger.debug("failed to get schedule status by v2, try use v1 later: schName={}",
                    schName, e);
        }
        InternalSchStatus ret = feign.getInternalSchLatestStatusV1(schName);
        if (!ret.isFinish()) {
            // 老版本的调度服务因为编码 BUG (该 BUG 在 SEQUOIACM-1215 提交中修复)，isFinish 总是返回 false，
            // 这里额外通过调度任务是否存在额外检查下这个属性（ finish 置为 true 时，调度任务会被删除）
            ret.setFinish(getInternalSchduleByName(schName) == null);
        }
        return ret;
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
