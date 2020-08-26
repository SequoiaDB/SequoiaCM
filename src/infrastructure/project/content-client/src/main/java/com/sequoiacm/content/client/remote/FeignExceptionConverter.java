package com.sequoiacm.content.client.remote;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;

public class FeignExceptionConverter implements ScmFeignExceptionConverter<ScmServerException> {

    @Override
    public ScmServerException convert(ScmFeignException e) {
        return new ScmServerException(ScmError.getScmError(e.getStatus()), e.getMessage());
    }

}
