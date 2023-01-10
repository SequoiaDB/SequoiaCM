package com.sequoiacm.schedule.remote;

import com.sequoiacm.schedule.common.model.TransitionEntityTranslator;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.IOException;
import java.lang.reflect.Type;

import static java.lang.String.format;

class TransitionScheduleEntityDecoder implements Decoder {
    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
        if (!TransitionScheduleEntity.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }

        Response.Body body = response.body();
        if (body == null) {
            return null;
        }

        String entity = Util.toString(body.asReader());
        BSONObject bson = (BSONObject) JSON.parse(entity);
        try {
            return TransitionEntityTranslator.WsFullInfo.fromDecoder(bson);
        }
        catch (Exception e) {
            throw new DecodeException(
                    format("encode TransitionScheduleEntity failed:entity=%s", entity), e);
        }
    }
}
