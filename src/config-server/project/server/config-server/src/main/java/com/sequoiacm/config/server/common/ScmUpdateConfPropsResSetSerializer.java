//package com.sequoiacm.config.server.common;
//
//import java.io.IOException;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import com.fasterxml.jackson.databind.ser.std.StdSerializer;
//import com.sequoiacm.config.core.common.ScmRestArgDefine;
//import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSet;
//
//public class ScmUpdateConfPropsResSetSerializer extends StdSerializer<ScmUpdateConfPropsResultSet> {
//
//    protected ScmUpdateConfPropsResSetSerializer() {
//        super(ScmUpdateConfPropsResultSet.class);
//    }
//
//    @Override
//    public void serialize(ScmUpdateConfPropsResultSet value, JsonGenerator gen,
//            SerializerProvider provider) throws IOException {
//        gen.writeStartObject();
//        gen.writeStringField(ScmRestArgDefine.CONF_PROPS_RES_SET_FAILES, provider.ser);
//        gen.writeStringField(ScmRestArgDefine.CONF_PROPS_RES_SET_SUCCESS, value.getServiceName());
//        gen.writeEndObject();
//    }
//
//}
