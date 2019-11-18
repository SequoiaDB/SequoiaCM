package com.sequoiacm.config.server.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmGsonTypeAdapter;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class ScmConfPropsParamGsonTypeAdapter
        extends ScmGsonTypeAdapter<String, ScmConfPropsParam> {
    private static final Logger logger = LoggerFactory
            .getLogger(ScmConfPropsParamGsonTypeAdapter.class);

    @Override
    public ScmConfPropsParam convert(String source) {
        //only use read() function.
        throw new RuntimeException("unsupport");
    }

    @Override
    public void write(JsonWriter out, ScmConfPropsParam value) throws IOException {
      //only use read() function.
        throw new RuntimeException("unsupport");
    }

    @Override
    public ScmConfPropsParam read(JsonReader in) throws IOException {
        String targetType = null;
        List<String> targets = new ArrayList<>();
        boolean isAcceptUnknownProps = false;
        Map<String, String> updateProps = new HashMap<>();
        List<String> deleteProps = new ArrayList<>();
        in.beginObject();
        while (in.hasNext()) {
            String key = in.nextName();
            switch (key) {
                case ScmRestArgDefine.CONF_PROPS_TARGET_TYPE:
                    targetType = in.nextString();
                    break;
                case ScmRestArgDefine.CONF_PROPS_ACCEPT_UNKNOWN_PROPS:
                    isAcceptUnknownProps = in.nextBoolean();
                    break;
                case ScmRestArgDefine.CONF_PROPS_TARGETS:
                    in.beginArray();
                    while (in.hasNext()) {
                        targets.add(in.nextString());
                    }
                    in.endArray();
                    break;
                case ScmRestArgDefine.CONF_PROPS_UPDATE_PROPERTIES:
                    in.beginObject();
                    while (in.hasNext()) {
                        updateProps.put(in.nextName(), in.nextString());
                    }
                    in.endObject();
                    break;
                case ScmRestArgDefine.CONF_PROPS_DELETE_PROPERTIES:
                    in.beginArray();
                    while(in.hasNext()) {
                        deleteProps.add(in.nextString());
                    }
                    in.endArray();
                    break;
                default:
                    throw new IOException("unknown argument:" + key);
            }
        }
        in.endObject();
        ScmConfPropsParam ret = new ScmConfPropsParam();
        ret.setAcceptUnrecognizedProp(isAcceptUnknownProps);
        ret.setUpdateProperties(updateProps);
        ret.setTargets(targets);
        ret.setTargetType(targetType);
        ret.setDeleteProperties(deleteProps);
        return ret;
    }
}
