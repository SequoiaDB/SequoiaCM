package com.sequoiacm.contentserver.remote;

import java.util.Collection;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiacm.contentserver.controller.RestExceptionHandler;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;

class ContentServerFeignExceptionConverter
implements ScmFeignExceptionConverter<ScmServerException> {
    @Override
    public ScmServerException convert(ScmFeignException e) {
        ScmServerException ret = new ScmServerException(ScmError.getScmError(e.getStatus()),
                e.getMessage(), e);
        Collection<String> extra = e.getHeaders().get(RestExceptionHandler.EXTRA_INFO_HEADER);
        if (extra != null && !extra.isEmpty()) {
            ret.setExtraInfo((BSONObject) JSON.parse(extra.iterator().next()));
        }
        return ret;
    }
}
