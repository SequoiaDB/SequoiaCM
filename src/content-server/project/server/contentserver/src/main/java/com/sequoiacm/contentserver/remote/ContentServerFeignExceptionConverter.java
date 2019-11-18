package com.sequoiacm.contentserver.remote;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;

class ContentServerFeignExceptionConverter
implements ScmFeignExceptionConverter<ScmServerException> {
    @Override
    public ScmServerException convert(ScmFeignException e) {
        return new ScmServerException(ScmError.getScmError(e.getStatus()), e.getMessage(), e);
    }
}
