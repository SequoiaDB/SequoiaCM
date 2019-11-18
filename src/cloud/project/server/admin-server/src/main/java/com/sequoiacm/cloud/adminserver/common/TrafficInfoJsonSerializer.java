package com.sequoiacm.cloud.adminserver.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.cloud.adminserver.model.TrafficInfo;

public class TrafficInfoJsonSerializer extends StdSerializer<TrafficInfo> {

    private static final long serialVersionUID = 3771458247952729145L;

    public TrafficInfoJsonSerializer() {
        super(TrafficInfo.class);
    }

    @Override
    public void serialize(TrafficInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.Traffic.FIELD_TYPE, value.getType());
        gen.writeStringField(FieldName.Traffic.FIELD_WORKSPACE_NAME, value.getWorkspaceName());
        gen.writeNumberField(FieldName.Traffic.FIELD_TRAFFIC, value.getTraffic());
        gen.writeNumberField(FieldName.Traffic.FIELD_RECORD_TIME, value.getRecordTime());
        gen.writeEndObject();
    }
}
