package com.sequoiadb.infrastructure.map.client.service;

import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

class MapFeignExceptionConverter implements ScmFeignExceptionConverter<ScmMapServerException> {
    @Override
    public ScmMapServerException convert(ScmFeignException e) {
        return new ScmMapServerException(ScmMapError.getScmError(e.getStatus()), e.getMessage(), e);
    }
}
