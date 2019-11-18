package com.sequoiacm.contentserver.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.common.FieldName;

public class MetadataClassJsonSerializer extends StdSerializer<MetadataClass> {

    private static final long serialVersionUID = -2755734172014922040L;

    private final ObjectMapper mapper;
    
    public MetadataClassJsonSerializer() {
        super(MetadataClass.class);
        SimpleModule module = new SimpleModule();
        module.addSerializer(MetadataAttr.class, new MetadataAttrJsonSerializer());
        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    @Override
    public void serialize(MetadataClass value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.Class.FIELD_ID, value.getId());
        gen.writeStringField(FieldName.Class.FIELD_NAME, value.getName());
        gen.writeStringField(FieldName.Class.FIELD_DESCRIPTION, value.getDescription());
        gen.writeStringField(FieldName.Class.FIELD_INNER_CREATE_USER, value.getCreateUser());
        gen.writeNumberField(FieldName.Class.FIELD_INNER_CREATE_TIME, value.getCreateTime());
        gen.writeStringField(FieldName.Class.FIELD_INNER_UPDATE_USER, value.getUpdateUser());
        gen.writeNumberField(FieldName.Class.FIELD_INNER_UPDATE_TIME, value.getUpdateTime());
        if (value.getAttrList() != null) {
            gen.writeObjectField(FieldName.Class.REL_ATTR_INFOS, value.getAttrList());
        }
        gen.writeEndObject();
    }
}
