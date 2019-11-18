package com.sequoiacm.contentserver.model.serial.gson;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;

public class MetadataClassGsonTypeAdapter extends ScmGsonTypeAdapter<String, MetadataClass> {

    private Gson gson;
    private MetadataAttrGsonTypeAdapter attrTypeAdpater = new MetadataAttrGsonTypeAdapter();

    public MetadataClassGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(MetadataClass.class, this);
        gson = gb.create();
    }

    @Override
    public MetadataClass convert(String source) {
        return gson.fromJson(source, MetadataClass.class);
    }

    @Override
    public void write(JsonWriter out, MetadataClass value) throws IOException {
        out.beginObject();
        out.name(FieldName.Class.FIELD_ID).value(value.getId());
        out.name(FieldName.Class.FIELD_NAME).value(value.getName());
        out.name(FieldName.Class.FIELD_DESCRIPTION).value(value.getDescription());
        out.name(FieldName.Class.FIELD_INNER_CREATE_USER).value(value.getCreateUser());
        out.name(FieldName.Class.FIELD_INNER_CREATE_TIME).value(value.getCreateTime());
        out.name(FieldName.Class.FIELD_INNER_UPDATE_USER).value(value.getUpdateUser());
        out.name(FieldName.Class.FIELD_INNER_UPDATE_TIME).value(value.getUpdateTime());

        writeAttrArray(out, FieldName.Class.REL_ATTR_INFOS, value.getAttrList());
        out.endObject();
    }

    private void writeAttrArray(JsonWriter out, String name, List<MetadataAttr> attrList)
            throws IOException {
        out.name(name);
        out.beginArray();

        if (null != attrList) {
            for (MetadataAttr a : attrList) {
                attrTypeAdpater.write(out, a);
            }
        }

        out.endArray();
    }

    @Override
    public MetadataClass read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }
}
