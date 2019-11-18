package com.sequoiacm.config.server.remote;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;

public class ScmConfServerExceptionConvertor
implements ScmFeignExceptionConverter<ScmConfigException> {

    @Override
    public ScmConfigException convert(ScmFeignException e) {
        return new ScmConfigException(ScmConfError.getScmError(e.getStatus()), e.getMessage(), e);
    }

}
