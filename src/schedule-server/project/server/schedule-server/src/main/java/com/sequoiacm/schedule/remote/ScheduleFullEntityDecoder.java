package com.sequoiacm.schedule.remote;

import com.sequoiacm.schedule.entity.ScheduleEntityTranslator;
import com.sequoiacm.schedule.entity.ScheduleFullEntity;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.IOException;
import java.lang.reflect.Type;

import static java.lang.String.format;

class ScheduleFullEntityDecoder implements Decoder {

    @Override
    public Object decode(Response response, Type type)
            throws IOException, DecodeException {
        if (!ScheduleFullEntity.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }

        Response.Body body = response.body();
        if (body == null) {
            return null;
        }

        String entity = Util.toString(body.asReader());
        BSONObject bson = (BSONObject)JSON.parse(entity);
        try {
            return ScheduleEntityTranslator.FullInfo.fromBSONObject(bson);
        }
        catch (Exception e) {
            throw new DecodeException(format("encode ScheduleFullEntity failed:entity=%s", entity));
        }
    }
}
