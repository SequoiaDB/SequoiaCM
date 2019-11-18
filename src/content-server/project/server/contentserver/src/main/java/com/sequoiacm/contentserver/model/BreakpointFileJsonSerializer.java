package com.sequoiacm.contentserver.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.common.FieldName;

import java.io.IOException;

public class BreakpointFileJsonSerializer extends StdSerializer<BreakpointFile> {

    public BreakpointFileJsonSerializer() {
        super(BreakpointFile.class);
    }

    @Override
    public void serialize(BreakpointFile value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.BreakpointFile.FIELD_FILE_NAME, value.getFileName());
        gen.writeStringField(FieldName.BreakpointFile.FIELD_SITE_NAME, value.getSiteName());
        gen.writeStringField(FieldName.BreakpointFile.FIELD_CHECKSUM_TYPE, value.getChecksumType().name());
        gen.writeNumberField(FieldName.BreakpointFile.FIELD_CHECKSUM, value.getChecksum());
        gen.writeStringField(FieldName.BreakpointFile.FIELD_DATA_ID, value.getDataId());
        gen.writeBooleanField(FieldName.BreakpointFile.FIELD_COMPLETED, value.isCompleted());
        gen.writeNumberField(FieldName.BreakpointFile.FIELD_UPLOAD_SIZE, value.getUploadSize());
        gen.writeStringField(FieldName.BreakpointFile.FIELD_CREATE_USER, value.getCreateUser());
        gen.writeNumberField(FieldName.BreakpointFile.FIELD_CREATE_TIME, value.getCreateTime());
        gen.writeStringField(FieldName.BreakpointFile.FIELD_UPLOAD_USER, value.getUploadUser());
        gen.writeNumberField(FieldName.BreakpointFile.FIELD_UPLOAD_TIME, value.getUploadTime());
        gen.writeEndObject();
    }
}
