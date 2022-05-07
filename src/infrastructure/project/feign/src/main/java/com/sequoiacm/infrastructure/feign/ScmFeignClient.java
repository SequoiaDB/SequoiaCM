package com.sequoiacm.infrastructure.feign;

import java.lang.reflect.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.infrastructure.common.NetUtil;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.feign.hystrix.ScmHystrixConfig;
import com.sequoiacm.infrastructure.feign.hystrix.ScmHystrixInvocationHandler;
import feign.*;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import feign.Request.Options;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.StringDecoder;
import feign.form.spring.SpringFormEncoder;
import feign.slf4j.Slf4jLogger;

// AutoConfig by resources/META-INF/spring.factories
@Component
@EnableScmServiceDiscoveryClient
public class ScmFeignClient implements ApplicationRunner {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(ScmFeignClient.class);

    @Autowired
    private ApplicationContext context;

    public static volatile boolean isApplicationStarted = false;
    public static String localService = null;
    public static String localHostPort = null;

    private static final SpringFormEncoder defaultEncoder = new SpringFormEncoder();
    private static ScmFeignErrorDecoder errorEncoder = null;
    private static final SimpleModule defaultModule = new SimpleModule();

    static {
        defaultModule.addDeserializer(BSONObject.class, new BSONObjectJsonDeserializer<>());
        defaultModule.addDeserializer(BasicBSONList.class,
                new BSONObjectJsonDeserializer<BasicBSONList>());
    }

    public ScmFeignClient(ScmHystrixConfig scmHystrixConfig, Environment environment)
            throws UnknownHostException {
        errorEncoder = new ScmFeignErrorDecoder(scmHystrixConfig.isEnabled());
        ScmFeignClient.localService = environment.getProperty("spring.application.name");
        String hostName = InetAddress.getLocalHost().getHostName();
        String port = environment.getProperty("server.port");
        ScmFeignClient.localHostPort = hostName + ":" + port;
    }

    public Builder builder() {
        return context.getBean(ScmFeignClient.Builder.class);
    }

    public static Builder builderForNotSpring() {
        return new Builder();
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        isApplicationStarted = true;
    }

    @Component
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public static class Builder {
        private ObjectMapper objectMapper = new ObjectMapper();
        private Map<Type, Decoder> typeDecoders = new HashMap<>();
        private Encoder encoder;
        private Options options;
        private ScmFeignExceptionConverter exceptionCoverter;
        private Logger.Level loggerLevel = Logger.Level.BASIC;

        // Spring sleuth will provide a Feign.Builer instance (prototype), this
        // builder can create a feign client that can be traced
        @Autowired(required = false)
        private feign.Feign.Builder springFeignBuilder;

        @Autowired
        private ScmServiceDiscoveryClient scmServiceDiscoveryClient;

        @Autowired
        private Client springClient;

        @Autowired
        private ScmFeignConfig scmFeignConfig;

        @Autowired
        private ScmHystrixConfig hystrixConfig;

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

        public <T> T serviceTarget(Class<T> apiType, final String serviceName) {
            Feign.Builder feignBuilder;
            if (springFeignBuilder != null) {
                feignBuilder = springFeignBuilder;
            }
            else {
                feignBuilder = Feign.builder().client(springClient);
            }
            if (hystrixConfig.isEnabled()) {
                feignBuilder.invocationHandlerFactory(new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler create(Target target,
                            Map<Method, MethodHandler> dispatch) {
                        return new ScmHystrixInvocationHandler(target, dispatch, serviceName,
                                serviceName);
                    }
                });
            }
            else {
                feignBuilder.invocationHandlerFactory(new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler create(Target target,
                            Map<Method, MethodHandler> dispatch) {
                        return new ScmFeignInvocationHandler(target, dispatch);
                    }
                });
            }
            return innerBuild(feignBuilder, apiType, serviceName);
        }

        public <T> T instanceTarget(Class<T> apiType, final String instanceHostPort) {
            Feign.Builder feignBuilder = Feign.builder();
            if (hystrixConfig.isEnabled()) {
                feignBuilder.invocationHandlerFactory(new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler create(Target target,
                            Map<Method, MethodHandler> dispatch) {
                        String service = scmServiceDiscoveryClient
                                .getServiceNameByUrl(instanceHostPort);
                        return new ScmHystrixInvocationHandler(target, dispatch,
                                service != null ? service : "unknown_service",
                                NetUtil.getHostAndPort(instanceHostPort));
                    }
                });
            }
            else {
                feignBuilder.invocationHandlerFactory(new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler create(Target target,
                            Map<Method, MethodHandler> dispatch) {
                        return new ScmFeignInvocationHandler(target, dispatch);
                    }
                });
            }
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
            else {
                feignBuilder.options(new Options(scmFeignConfig.getConnectTimeout(),
                        scmFeignConfig.getReadTimeout()));
            }

            if (exceptionCoverter != null) {
                feignBuilder.errorDecoder(
                        new ScmFeignErrorDecoder(exceptionCoverter, hystrixConfig.isEnabled()));
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
