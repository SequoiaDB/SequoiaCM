package com.sequoiacm.config.server.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class ConfigSerializer extends StdSerializer<Config> {

    public ConfigSerializer() {
        super(Config.class);
    }

    @Override
    public void serialize(Config value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            return;
        }
        gen.writeObject(value.toBSONObject());
    }

}