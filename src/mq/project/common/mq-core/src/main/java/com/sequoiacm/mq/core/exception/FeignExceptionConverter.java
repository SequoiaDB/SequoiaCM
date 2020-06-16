package com.sequoiacm.mq.core.exception;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;

public class FeignExceptionConverter implements ScmFeignExceptionConverter<MqException> {

    @Override
    public MqException convert(ScmFeignException e) {
        return new MqException(MqError.convertToMqError(e.getStatus()), e.getMessage());
    }

}
