package com.sequoiacm.config.server.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.infrastructure.config.core.msg.Version;

public class VersionSerializer extends StdSerializer<Version> {

    public VersionSerializer() {
        super(Version.class);
    }

    @Override
    public void serialize(Version value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            return;
        }
        gen.writeObject(value.toBSONObject());
    }

}