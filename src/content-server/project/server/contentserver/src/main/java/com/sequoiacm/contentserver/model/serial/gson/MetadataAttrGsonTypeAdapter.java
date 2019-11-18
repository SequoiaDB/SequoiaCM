package com.sequoiacm.contentserver.model.serial.gson;

import java.io.IOException;
import java.io.StringWriter;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.MetadataAttr;

public class MetadataAttrGsonTypeAdapter extends ScmGsonTypeAdapter<String, MetadataAttr> {

    private Gson gson;

    public MetadataAttrGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(MetadataAttr.class, this);
        gson = gb.create();
    }

    @Override
    public MetadataAttr convert(String source) {
        return gson.fromJson(source, MetadataAttr.class);
    }

    @Override
    public void write(JsonWriter out, MetadataAttr value) throws IOException {
        out.beginObject();
        out.name(FieldName.Attribute.FIELD_ID).value(value.getId());
        out.name(FieldName.Attribute.FIELD_NAME).value(value.getName());
        out.name(FieldName.Attribute.FIELD_DISPLAY_NAME).value(value.getDisplayName());
        out.name(FieldName.Attribute.FIELD_DESCRIPTION).value(value.getDescription());
        out.name(FieldName.Attribute.FIELD_TYPE).value(value.getType().getName());
        BSONObject o = value.getCheckRule();
        if (null != o) {
            out.name(FieldName.Attribute.FIELD_CHECK_RULE).jsonValue(o.toString());
        }
        else {
            out.name(FieldName.Attribute.FIELD_CHECK_RULE).nullValue();
        }

        out.name(FieldName.Attribute.FIELD_REQUIRED).value(value.isRequired());
        out.name(FieldName.Attribute.FIELD_INNER_CREATE_USER).value(value.getCreateUser());
        out.name(FieldName.Attribute.FIELD_INNER_CREATE_TIME).value(value.getCreateTime());
        out.name(FieldName.Attribute.FIELD_INNER_UPDATE_USER).value(value.getUpdateUser());
        out.name(FieldName.Attribute.FIELD_INNER_UPDATE_TIME).value(value.getUpdateTime());
        out.endObject();
    }

    @Override
    public MetadataAttr read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }

    public static void main(String[] args) throws IOException {
        MetadataAttr v = new MetadataAttr();
        BSONObject o = new BasicBSONObject();
        o.put("k1", "v");
        o.put("$sadf", 12);
        v.setCheckRule(o);
        v.setType(AttributeType.BOOLEAN);
        MetadataAttrGsonTypeAdapter m = new MetadataAttrGsonTypeAdapter();

        StringWriter writer = new StringWriter();
        JsonWriter out = new JsonWriter(writer);
        m.write(out, v);

        System.out.println(writer.toString());
    }
}
