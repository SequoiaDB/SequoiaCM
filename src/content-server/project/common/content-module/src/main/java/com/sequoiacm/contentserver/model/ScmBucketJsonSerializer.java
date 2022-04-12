package com.sequoiacm.contentserver.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.common.FieldName;

import java.io.IOException;

public class ScmBucketJsonSerializer extends StdSerializer<ScmBucket> {

    public ScmBucketJsonSerializer() {
        super(ScmBucket.class);
    }

    @Override
    public void serialize(ScmBucket value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.Bucket.NAME, value.getName());
        gen.writeNumberField(FieldName.Bucket.ID, value.getId());
        gen.writeStringField(FieldName.Bucket.CREATE_USER, value.getCreateUser());
        gen.writeStringField(FieldName.Bucket.WORKSPACE, value.getWorkspace());
        gen.writeNumberField(FieldName.Bucket.CREATE_TIME, value.getCreateTime());
        gen.writeEndObject();
    }
}
