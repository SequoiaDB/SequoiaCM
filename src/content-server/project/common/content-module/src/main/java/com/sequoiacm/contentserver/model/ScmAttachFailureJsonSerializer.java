package com.sequoiacm.contentserver.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.module.ScmBucketAttachFailure;

import java.io.IOException;

public class ScmAttachFailureJsonSerializer extends StdSerializer<ScmBucketAttachFailure> {

    public ScmAttachFailureJsonSerializer() {
        super(ScmBucketAttachFailure.class);
    }

    @Override
    public void serialize(ScmBucketAttachFailure value, JsonGenerator gen,
            SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(CommonDefine.RestArg.ATTACH_FAILURE_FILE_ID, value.getFileId());
        gen.writeNumberField(CommonDefine.RestArg.ATTACH_FAILURE_ERROR_CODE,
                value.getError().getErrorCode());
        gen.writeStringField(CommonDefine.RestArg.ATTACH_FAILURE_ERROR_MSG, value.getMessage());
        if (value.getExternalInfo() != null) {
            gen.writeObjectField(CommonDefine.RestArg.ATTACH_FAILURE_EXT_INFO,
                    value.getExternalInfo());
        }
        gen.writeEndObject();
    }
}
