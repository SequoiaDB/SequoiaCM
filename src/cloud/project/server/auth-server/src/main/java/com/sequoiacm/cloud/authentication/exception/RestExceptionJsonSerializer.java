package com.sequoiacm.cloud.authentication.exception;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class RestExceptionJsonSerializer extends StdSerializer<RestException> {

    public RestExceptionJsonSerializer() {
        super(RestException.class);
    }

    @Override
    public void serialize(RestException value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("timestamp", value.getTimestamp());
        gen.writeNumberField("status", value.getStatus());
        gen.writeStringField("error", value.getError());
        gen.writeStringField("exception", value.getException());
        gen.writeStringField("message", value.getMessage());
        gen.writeStringField("path", value.getPath());
        gen.writeEndObject();
    }
}
