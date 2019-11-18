package com.sequoiacm.cloud.adminserver.remote;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;

class ContentServerFeignExceptionConverter implements
ScmFeignExceptionConverter<StatisticsException> {
    @Override
    public StatisticsException convert(ScmFeignException e) {
        StatisticsException ex = new StatisticsException(e.getError(), e.getMessage(), e);
        ex.setLocation(e.getPath());
        return ex;
    }
}
