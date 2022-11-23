package com.sequoiacm.contentserver.model.serial.gson;

import java.io.IOException;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class ScmFileLocationGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmFileLocation> {

    private Gson gson;

    public ScmFileLocationGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(BreakpointFile.class, this);
        gson = gb.create();
    }

    @Override
    public ScmFileLocation convert(String source) {
        return gson.fromJson(source, ScmFileLocation.class);
    }

    @Override
    public void write(JsonWriter out, ScmFileLocation value) throws IOException {
        out.beginObject();
        out.name(FieldName.ScmFileLocation.SITE_ID).value(value.getSiteId());
        out.name(FieldName.ScmFileLocation.DATE).value(value.getDate().getTime());
        out.name(FieldName.ScmFileLocation.CREATE_DATE).value(value.getCreateDate().getTime());
        out.name(FieldName.ScmFileLocation.WS_VERSION).value(value.getWsVersion());
        out.endObject();
    }

    @Override
    public ScmFileLocation read(JsonReader in) throws IOException {
        int siteId = 0;
        int wsVersion = 0;
        long date = 0;
        long createDate = 0;

        in.beginObject();
        while (in.hasNext()) {
            String key = in.nextName();
            switch (key) {
                case FieldName.ScmFileLocation.SITE_ID:
                    siteId = in.nextInt();
                    break;
                case FieldName.ScmFileLocation.DATE:
                    date = in.nextLong();
                    break;
                case FieldName.ScmFileLocation.CREATE_DATE:
                    createDate = in.nextLong();
                    break;
                case FieldName.ScmFileLocation.WS_VERSION:
                    wsVersion = in.nextInt();
                    break;
                default:
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        return new ScmFileLocation(siteId, date, createDate, wsVersion);
    }
}
