package com.sequoiacm.infrastructure.feign;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Request.Options;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.StringDecoder;
import feign.form.spring.SpringFormEncoder;
import feign.slf4j.Slf4jLogger;

// AutoConfig by resources/META-INF/spring.factories
@Component
public class ScmFeignClient {

    @Autowired
    private ApplicationContext context;

    private static final SpringFormEncoder defaultEncoder = new SpringFormEncoder();
    private static final ScmFeignErrorDecoder errorEncoder = new ScmFeignErrorDecoder();
    private static final SimpleModule defaultModule = new SimpleModule();

    static {
        defaultModule.addDeserializer(BSONObject.class, new BSONObjectJsonDeserializer<>());
        defaultModule.addDeserializer(BasicBSONList.class,
                new BSONObjectJsonDeserializer<BasicBSONList>());
    }

    public Builder builder() {
        return context.getBean(ScmFeignClient.Builder.class);
    }

    @Component
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public static class Builder {
        private ObjectMapper objectMapper = new ObjectMapper();
        private Map<Type, Decoder> typeDecoders = new HashMap<>();
        private Encoder encoder;
        private Options options;
        private ScmFeignExceptionConverter exceptionCoverter;
        private Logger.Level loggerLevel = Logger.Level.FULL;

        // Spring sleuth will provide a Feign.Builer instance (prototype), this
        // builder can create a feign client that can be traced
        @Autowired(required = false)
        private feign.Feign.Builder springFeignBuilder;

        @Autowired
        private Client springClient;

        private Builder() {
        }

        public Builder encoder(Encoder encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder options(Request.Options options) {
            this.options = options;
            return this;
        }

        public Builder typeDecoder(Type type, Decoder decoder) {
            typeDecoders.put(type, decoder);
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder exceptionConverter(ScmFeignExceptionConverter exceptionConverter) {
            this.exceptionCoverter = exceptionConverter;
            return this;
        }

        public <T> T serviceTarget(Class<T> apiType, String serviceName) {
            Feign.Builder feignBuilder;
            if (springFeignBuilder != null) {
                feignBuilder = springFeignBuilder;
            }
            else {
                feignBuilder = Feign.builder().client(springClient);
            }
            return innerBuild(feignBuilder, apiType, serviceName);
        }

        public <T> T instanceTarget(Class<T> apiType, String instanceHostPort) {
            Feign.Builder feignBuilder = Feign.builder();
            return innerBuild(feignBuilder, apiType, instanceHostPort);
        }

        // NOTE: if the client will receive an large response, please specify
        // the loggerLevel to BASIC or NONE
        public Builder loggerLevel(Logger.Level level) {
            this.loggerLevel = level;
            return this;
        }

        private <T> T innerBuild(Feign.Builder feignBuilder, Class<T> apiType, String url) {
            if (encoder != null) {
                feignBuilder.encoder(encoder);
            }
            else {
                feignBuilder.encoder(defaultEncoder);
            }

            if (options != null) {
                feignBuilder.options(options);
            }

            if (exceptionCoverter != null) {
                feignBuilder.errorDecoder(new ScmFeignErrorDecoder(exceptionCoverter));
            }
            else {
                feignBuilder.errorDecoder(errorEncoder);
            }

            feignBuilder.contract(new SpringMvcContract()).retryer(Retryer.NEVER_RETRY)
                    .logLevel(loggerLevel);

            typeDecoders.put(String.class, new StringDecoder());
            objectMapper.configure(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS, true);
            objectMapper.registerModule(defaultModule);
            ScmFeignDecoder decoder = new ScmFeignDecoder(objectMapper,
                    typeDecoders.isEmpty() ? null : typeDecoders);
            feignBuilder.logger(new Slf4jLogger(apiType)).decoder(decoder);
            return feignBuilder.target(apiType, "http://" + url);
        }
    }
}
