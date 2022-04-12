package com.sequoiacm.contentserver.remote;

import java.util.Collection;
import java.util.Map;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;

import feign.Response;

public class RemoteCommonUtil {
    private final static ScmFeignErrorDecoder errDecoder = new ScmFeignErrorDecoder(
            new ContentServerFeignExceptionConverter());

    public static void checkResponse(String methodKey, Response response) throws ScmServerException {
        if (response.status() >= 200 && response.status() < 300) {
            return;
        }

        Exception e = errDecoder.decode(methodKey, response);
        if (e instanceof ScmServerException) {
            throw (ScmServerException) e;
        } else {
            throw new ScmSystemException(e.getMessage(), e);
        }
    }

    public static <T> T firstOrNull(Map<String, Collection<T>> map, String key) {
        if (map.containsKey(key) && !map.get(key).isEmpty()) {
            return map.get(key).iterator().next();
        }
        return null;
    }
}
