package com.sequoiacm.config.server.module;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmGsonTypeAdapter;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class ScmUpdateConfPropsResultSetGsonTypeAdapter
        extends ScmGsonTypeAdapter<String, ScmUpdateConfPropsResultSet> {

    public static final Logger logger = LoggerFactory
            .getLogger(ScmUpdateConfPropsResultSetGsonTypeAdapter.class);

    @Override
    public ScmUpdateConfPropsResultSet convert(String source) {
        // only use write() function.
        throw new RuntimeException("unsupport");
    }

    @Override
    public void write(JsonWriter out, ScmUpdateConfPropsResultSet value) throws IOException {
        out.beginObject();

        out.name(ScmRestArgDefine.CONF_PROPS_RES_SET_SUCCESS);
        out.beginArray();
        for (ScmUpdateConfPropsResult success : value.getSuccesses()) {
            out.beginObject();
            out.name(ScmRestArgDefine.CONF_PROPS_RES_SERVICE);
            out.value(success.getServiceName());
            out.name(ScmRestArgDefine.CONF_PROPS_RES_INSTANCE);
            out.value(success.getInstanceUrl());
            out.endObject();
        }
        out.endArray();

        out.name(ScmRestArgDefine.CONF_PROPS_RES_SET_FAILES);
        out.beginArray();
        for (ScmUpdateConfPropsResult fail : value.getFailes()) {
            out.beginObject();
            out.name(ScmRestArgDefine.CONF_PROPS_RES_SERVICE);
            out.value(fail.getServiceName());
            out.name(ScmRestArgDefine.CONF_PROPS_RES_INSTANCE);
            out.value(fail.getInstanceUrl());
            out.name(ScmRestArgDefine.CONF_PROPS_RES_MESSAGE);
            out.value(fail.getErrorMessage());
            out.endObject();
        }
        out.endArray();

        out.name(ScmRestArgDefine.CONF_PROPS_REBOOT_CONF);
        out.beginArray();
        for (String conf : value.getRebootConf()) {
            out.value(conf);
        }
        out.endArray();

        out.name(ScmRestArgDefine.CONF_PROPS_ADJUST_CONF);
        out.beginObject();
        for (Map.Entry<String, String> entry : value.getAdjustConf().entrySet()) {
            out.name(entry.getKey());
            out.value(entry.getValue());
        }
        out.endObject();

        out.endObject();
    }

    @Override
    public ScmUpdateConfPropsResultSet read(JsonReader in) throws IOException {
        // only use write() function.
        throw new RuntimeException("unsupport");
    }

}
