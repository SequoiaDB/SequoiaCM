package com.sequoiacm.schedule.common;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;
import com.sequoiacm.schedule.common.model.ScheduleException;

public class ScheduleExceptionConverter implements ScmFeignExceptionConverter<ScheduleException> {

    @Override
    public ScheduleException convert(ScmFeignException e) {
        ScheduleException ret = new ScheduleException(e.getError(), e.getMessage());
        ret.setLocation(e.getPath());
        return ret;
    }

}
