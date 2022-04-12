package com.sequoiacm.contentserver.common;

import java.io.IOException;

import org.bson.BSONObject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class BSONObjectJsonSerializer extends StdSerializer<BSONObject> {

    public BSONObjectJsonSerializer() {
        super(BSONObject.class);
    }

    private static final long serialVersionUID = -4462739745788126906L;

    @Override
    public void serialize(BSONObject value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeObject(value);
    }

}
