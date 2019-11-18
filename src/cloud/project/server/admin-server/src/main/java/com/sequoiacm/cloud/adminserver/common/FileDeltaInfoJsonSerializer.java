package com.sequoiacm.cloud.adminserver.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;

public class FileDeltaInfoJsonSerializer extends StdSerializer<FileDeltaInfo> {

    private static final long serialVersionUID = 3771458247952729145L;

    public FileDeltaInfoJsonSerializer() {
        super(FileDeltaInfo.class);
    }

    @Override
    public void serialize(FileDeltaInfo value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.FileDelta.FIELD_WORKSPACE_NAME, value.getWorkspaceName());
        gen.writeNumberField(FieldName.FileDelta.FIELD_COUNT_DELTA, value.getCountDelta());
        gen.writeNumberField(FieldName.FileDelta.FIELD_SIZE_DELTA, value.getSizeDelta());
        gen.writeNumberField(FieldName.FileDelta.FIELD_RECORD_TIME, value.getRecordTime());
        gen.writeEndObject();
    }
}
