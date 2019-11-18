package com.sequoiacm.contentserver.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.common.FieldName;

public class MetadataAttrJsonSerializer extends StdSerializer<MetadataAttr> {

    private static final long serialVersionUID = 3771458247952729145L;

    public MetadataAttrJsonSerializer() {
        super(MetadataAttr.class);
    }

    @Override
    public void serialize(MetadataAttr value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.Attribute.FIELD_ID, value.getId());
        gen.writeStringField(FieldName.Attribute.FIELD_NAME, value.getName());
        gen.writeStringField(FieldName.Attribute.FIELD_DISPLAY_NAME, value.getDisplayName());
        gen.writeStringField(FieldName.Attribute.FIELD_DESCRIPTION, value.getDescription());
        gen.writeStringField(FieldName.Attribute.FIELD_TYPE, value.getType().getName());
        gen.writeObjectField(FieldName.Attribute.FIELD_CHECK_RULE, value.getCheckRule());
        gen.writeBooleanField(FieldName.Attribute.FIELD_REQUIRED, value.isRequired());
        gen.writeStringField(FieldName.Attribute.FIELD_INNER_CREATE_USER, value.getCreateUser());
        gen.writeNumberField(FieldName.Attribute.FIELD_INNER_CREATE_TIME, value.getCreateTime());
        gen.writeStringField(FieldName.Attribute.FIELD_INNER_UPDATE_USER, value.getUpdateUser());
        gen.writeNumberField(FieldName.Attribute.FIELD_INNER_UPDATE_TIME, value.getUpdateTime());
        gen.writeEndObject();
    }
}
