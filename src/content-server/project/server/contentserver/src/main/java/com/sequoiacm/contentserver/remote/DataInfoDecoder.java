package com.sequoiacm.contentserver.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.common.CommonDefine;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;

class DataInfoDecoder implements Decoder {
    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException {
        if (!DataInfo.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }

        String dataInfoStr = RemoteCommonUtil.firstOrNull(response.headers(), CommonDefine.RestArg.DATASOURCE_DATA_HEADER);
        if(dataInfoStr == null) {
            throw new DecodeException("Failed to decode data info, missing header:header=" + CommonDefine.RestArg.DATASOURCE_DATA_HEADER);
        }
        return mapper.readValue(dataInfoStr, DataInfo.class);
    }
}
