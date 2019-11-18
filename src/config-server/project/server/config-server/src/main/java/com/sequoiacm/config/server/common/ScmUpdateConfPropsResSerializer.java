//package com.sequoiacm.config.server.common;
//
//import java.io.IOException;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import com.fasterxml.jackson.databind.ser.std.StdSerializer;
//import com.sequoiacm.config.core.common.ScmRestArgDefine;
//import com.sequoiacm.config.server.module.ScmUpdateConfPropsResult;
//
//public class ScmUpdateConfPropsResSerializer extends StdSerializer<ScmUpdateConfPropsResult> {
//
//    protected ScmUpdateConfPropsResSerializer() {
//        super(ScmUpdateConfPropsResult.class);
//    }
//
//    @Override
//    public void serialize(ScmUpdateConfPropsResult value, JsonGenerator gen,
//            SerializerProvider provider) throws IOException {
//        gen.writeStartObject();
//        gen.writeStringField(ScmRestArgDefine.CONF_PROPS_RES_SERVICE, value.getServiceName());
//        gen.writeStringField(ScmRestArgDefine.CONF_PROPS_RES_INSTANCE, value.getInstanceUrl());
//        gen.writeStringField(ScmRestArgDefine.CONF_PROPS_RES_MESSAGE, value.getErrorMessage());
//        gen.writeEndObject();
//    }
//
//}
