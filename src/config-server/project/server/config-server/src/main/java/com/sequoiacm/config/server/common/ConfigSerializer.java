package com.sequoiacm.config.server.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;

public class ConfigSerializer extends StdSerializer<Config> {

    private final ConfigEntityTranslator configEntityTranslator;

    public ConfigSerializer(ConfigEntityTranslator configEntityTranslator) {
        super(Config.class);
        this.configEntityTranslator = configEntityTranslator;
    }

    @Override
    public void serialize(Config value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            return;
        }
        gen.writeObject(configEntityTranslator.toConfigBSON(value));
    }

}