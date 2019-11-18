package com.sequoiacm.infrastructure.feign;

public interface ScmFeignExceptionConverter<E extends Exception> {
    E convert(ScmFeignException e);
}
