package com.sequoiacm.cloud.adminserver.remote;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.common.RestCommonDefine;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;

import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

class FileDeltaInfoDecoder implements Decoder {

    private static final Logger logger = LoggerFactory.getLogger(FileDeltaInfoDecoder.class);
    @Override
    public Object decode(Response response, Type type) throws DecodeException {
        if (!FileDeltaInfo.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }

        Map<String, Collection<String>> headers = response.headers();
        logger.debug("file delta headers: {}", headers);
        if (headers == null) {
            return null;
        }
        
        try {
            String count = headers.get(RestCommonDefine.X_SCM_COUNT).iterator().next();
            String sum = headers.get(RestCommonDefine.X_SCM_SUM).iterator().next();
            FileDeltaInfo fileDeltaInfo = new FileDeltaInfo();
            fileDeltaInfo.setCountDelta(Long.parseLong(count));
            fileDeltaInfo.setSizeDelta(Long.parseLong(sum));
            return fileDeltaInfo;
        }
        catch (Exception e) {
            throw new DecodeException(String.format("encode file delta Map failed:headers=%s", headers));
        }
    }
}
