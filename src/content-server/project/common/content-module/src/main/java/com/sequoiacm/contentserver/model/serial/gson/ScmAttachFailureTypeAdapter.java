package com.sequoiacm.contentserver.model.serial.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.contentserver.model.BreakpointFile;

import java.io.IOException;

public class ScmAttachFailureTypeAdapter
        extends ScmGsonTypeAdapter<String, ScmBucketAttachFailure> {

    private Gson gson;

    public ScmAttachFailureTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(BreakpointFile.class, this);
        gson = gb.create();
    }

    @Override
    public ScmBucketAttachFailure convert(String source) {
        return gson.fromJson(source, ScmBucketAttachFailure.class);
    }

    @Override
    public void write(JsonWriter out, ScmBucketAttachFailure value) throws IOException {
        out.beginObject();
        out.name(CommonDefine.RestArg.ATTACH_FAILURE_FILE_ID).value(value.getFileId());
        out.name(CommonDefine.RestArg.ATTACH_FAILURE_ERROR_CODE)
                .value(value.getError().getErrorCode());
        out.name(CommonDefine.RestArg.ATTACH_FAILURE_ERROR_MSG).value(value.getMessage());
        if (value.getExternalInfo() != null) {
            out.name(CommonDefine.RestArg.ATTACH_FAILURE_EXT_INFO)
                    .jsonValue(value.getExternalInfo().toString());
        }
        out.endObject();
    }

    @Override
    public ScmBucketAttachFailure read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }
}
