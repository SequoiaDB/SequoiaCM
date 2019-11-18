package com.sequoiacm.cloud.adminserver.remote;

import java.lang.reflect.Type;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.model.HealthInfo;

import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;

class HealthInfoDecoder implements Decoder {

    private static final Logger logger = LoggerFactory.getLogger(HealthInfoDecoder.class);

    @Override
    public Object decode(Response response, Type type) throws DecodeException {
        if (!HealthInfo.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }
        Response.Body body = response.body();
        if (body == null) {
            return null;
        }
        HealthInfo hs = new HealthInfo();
        try {
            String entity = Util.toString(body.asReader());
            BSONObject bson = (BSONObject) JSON.parse(entity);
            String status = bson.get("status").toString();
            hs.setStatus(status);
            return hs;
        }
        catch (Exception e) {
            throw new DecodeException(" fail to decode health info", e);
        }
    }
}