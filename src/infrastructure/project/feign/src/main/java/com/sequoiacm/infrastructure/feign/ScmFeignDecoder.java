package com.sequoiacm.infrastructure.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class ScmFeignDecoder implements Decoder {
    private JacksonDecoder jacksonDecoder = new JacksonDecoder();
    private Map<Type, Decoder> typeDecoders = Collections.emptyMap();

    public ScmFeignDecoder(ObjectMapper mapper, Map<Type, Decoder> typeDecoders) {
        if (mapper != null) {
            this.jacksonDecoder = new JacksonDecoder(mapper);
        }

        if (typeDecoders != null) {
            this.typeDecoders = typeDecoders;
        }
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        Decoder decoder = typeDecoders.get(type);
        if (decoder == null) {
            decoder = jacksonDecoder;
        }

        return decoder.decode(response, type);
    }
}
