package com.sequoiacm.om.omserver.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.om.omserver.module.OmScheduleInfo;

import static com.sequoiacm.om.omserver.common.RestParamDefine.*;

public class ScheduleInfoJsonSerializer extends StdSerializer<OmScheduleInfo> {

    private static final long serialVersionUID = 9175155557141710265L;

    public ScheduleInfoJsonSerializer() {
        super(OmScheduleInfo.class);
    }

    @Override
    public void serialize(OmScheduleInfo value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(SCHEDULE_ID, value.getScheduleId());
        gen.writeStringField(SCHEDULE_NAME, value.getName());
        gen.writeStringField(SCHEDULE_TYPE, value.getType());
        gen.writeStringField(SCHEDULE_WORKSPACE, value.getWorkspace());
        gen.writeBooleanField(SCHEDULE_ENABLE, value.getEnable());
        gen.writeStringField(SCHEDULE_CRON, value.getCron());
        gen.writeStringField(SCHEDULE_PREFERRED_REGION, value.getPreferredRegion());
        gen.writeStringField(SCHEDULE_PREFERRED_ZONE, value.getPreferredZone());
        gen.writeObjectField(SCHEDULE_CREATE_TIME, value.getCreateTime());
        gen.writeStringField(SCHEDULE_CREATE_USER, value.getCreateUser());
        gen.writeObjectField(SCHEDULE_CONTENT, value.getContent());
        gen.writeStringField(SCHEDULE_DESCRIPTION, value.getDescription());
        gen.writeEndObject();
    }
}