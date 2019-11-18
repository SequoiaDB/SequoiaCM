package com.sequoiacm.config.server.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class ScmSubscriberSerializer extends StdSerializer<ScmConfSubscriber> {

    public ScmSubscriberSerializer() {
        super(ScmConfSubscriber.class);
    }

    @Override
    public void serialize(ScmConfSubscriber value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ScmRestArgDefine.CONFIG_NAME, value.getConfigName());
        gen.writeStringField(ScmRestArgDefine.SERVICE_NAME, value.getServiceName());
        gen.writeEndObject();
    }

}