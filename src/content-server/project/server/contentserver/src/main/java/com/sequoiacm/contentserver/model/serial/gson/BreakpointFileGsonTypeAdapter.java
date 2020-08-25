package com.sequoiacm.contentserver.model.serial.gson;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.BreakpointFile;

public class BreakpointFileGsonTypeAdapter extends ScmGsonTypeAdapter<String, BreakpointFile> {

    private Gson gson;

    public BreakpointFileGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(BreakpointFile.class, this);
        gson = gb.create();
    }

    @Override
    public BreakpointFile convert(String source) {
        return gson.fromJson(source, BreakpointFile.class);
    }

    @Override
    public void write(JsonWriter out, BreakpointFile value) throws IOException {
        out.beginObject();
        out.name(FieldName.BreakpointFile.FIELD_FILE_NAME).value(value.getFileName());
        out.name(FieldName.BreakpointFile.FIELD_SITE_NAME).value(value.getSiteName());
        out.name(FieldName.BreakpointFile.FIELD_CHECKSUM_TYPE)
                .value(value.getChecksumType().name());
        out.name(FieldName.BreakpointFile.FIELD_CHECKSUM).value(value.getChecksum());
        out.name(FieldName.BreakpointFile.FIELD_DATA_ID).value(value.getDataId());
        out.name(FieldName.BreakpointFile.FIELD_COMPLETED).value(value.isCompleted());
        out.name(FieldName.BreakpointFile.FIELD_UPLOAD_SIZE).value(value.getUploadSize());
        out.name(FieldName.BreakpointFile.FIELD_CREATE_USER).value(value.getCreateUser());
        out.name(FieldName.BreakpointFile.FIELD_CREATE_TIME).value(value.getCreateTime());
        out.name(FieldName.BreakpointFile.FIELD_UPLOAD_USER).value(value.getUploadUser());
        out.name(FieldName.BreakpointFile.FIELD_UPLOAD_TIME).value(value.getUploadTime());
        out.name(FieldName.BreakpointFile.FIELD_IS_NEED_MD5).value(value.isNeedMd5());
        out.name(FieldName.BreakpointFile.FIELD_MD5).value(value.getMd5());
        out.endObject();
    }

    @Override
    public BreakpointFile read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }
}
