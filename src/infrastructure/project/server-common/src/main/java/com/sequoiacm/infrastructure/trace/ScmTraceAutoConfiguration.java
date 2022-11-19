package com.sequoiacm.infrastructure.trace;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.instrument.web.TraceFilter;
import org.springframework.cloud.sleuth.instrument.zuul.TracePreZuulFilter;
import org.springframework.cloud.sleuth.sampler.NeverSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
public class ScmTraceAutoConfiguration {

    @Bean
    public HttpSpanInjector httpSpanInjector() {
        return new ScmHttpSpanInjector();
    }

    @Bean
    public SpanAdjuster scmSpanAdjuster() {
        return new ScmSpanAdjuster();
    }

    @Bean
    @ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
    public ScmTracePostFilter scmTracePostFilter() {
        return new ScmTracePostFilter();
    }

    @Bean
    @ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
    public ScmTracePreFilter scmTracePreFilter() {
        return new ScmTracePreFilter();
    }

    /**
     * 网关
     */
    @Configuration
    @EnableConfigurationProperties(ScmTraceConfig.class)
    @ConditionalOnClass(name = "com.netflix.zuul.ZuulFilter")
    static class GatewayTraceConfiguration {

        @Bean
        @RefreshScope
        public Sampler scmPercentageAndPathBaseSampler(ScmTraceConfig config) {
            return new ScmPercentageAndPathBaseSampler(config);
        }

        @Bean
        @ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
        public TraceFilter scmGatewayTraceFilter(BeanFactory beanFactory,
                ScmTraceConfig traceConfig, Tracer tracer) {
            return new ScmGatewayTraceFilter(beanFactory, traceConfig, tracer);
        }

        @Bean
        @ConditionalOnProperty(value = "spring.sleuth.zuul.enabled", matchIfMissing = true)
        public TracePreZuulFilter scmTracePreZuulFilter(Tracer tracer,
                HttpSpanInjector spanInjector, HttpTraceKeysInjector httpTraceKeysInjector,
                ErrorParser errorParser) {
            return new ScmTracePreZuulFilter(tracer, spanInjector, httpTraceKeysInjector,
                    errorParser);
        }

    }

    /**
     * 其它服务
     */
    @Configuration
    @ConditionalOnMissingClass("com.netflix.zuul.ZuulFilter")
    static class OtherNodesTraceConfiguration {

        @Bean
        public Sampler neverSampler() {
            return NeverSampler.INSTANCE;
        }

        @Bean
        @ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
        public TraceFilter scmOtherNodeTracerFilter(BeanFactory beanFactory) {
            return new ScmOtherNodeTracerFilter(beanFactory);
        }

    }

}
