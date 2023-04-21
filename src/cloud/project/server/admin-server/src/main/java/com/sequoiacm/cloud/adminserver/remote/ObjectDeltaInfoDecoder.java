package com.sequoiacm.cloud.adminserver.remote;

import com.sequoiacm.cloud.adminserver.model.ObjectDeltaInfo;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
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

public class ObjectDeltaInfoDecoder implements Decoder {
    @Override
    public Object decode(Response response, Type type)
            throws IOException, DecodeException, FeignException {
        if (!ObjectDeltaInfo.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }

        Response.Body body = response.body();
        if (body == null) {
            return null;
        }

        String entity = Util.toString(body.asReader());
        BSONObject bson = (BSONObject) JSON.parse(entity);
        try {
            ObjectDeltaInfo objectDeltaInfo = new ObjectDeltaInfo();
            objectDeltaInfo.setCountDelta(BsonUtils
                    .getNumberChecked(bson, FieldName.ObjectDelta.FIELD_COUNT_DELTA).longValue());
            objectDeltaInfo.setSizeDelta(BsonUtils
                    .getNumberChecked(bson, FieldName.ObjectDelta.FIELD_SIZE_DELTA).longValue());
            objectDeltaInfo.setBucketName(
                    BsonUtils.getStringChecked(bson, FieldName.ObjectDelta.FIELD_BUCKET_NAME));
            return objectDeltaInfo;
        }
        catch (Exception e) {
            throw new DecodeException(format("encode ObjectDeltaInfo failed:entity=%s", entity));
        }
    }
}
