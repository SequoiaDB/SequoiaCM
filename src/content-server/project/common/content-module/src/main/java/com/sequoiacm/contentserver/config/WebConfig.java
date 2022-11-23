package com.sequoiacm.contentserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.MetadataAttr;
import com.sequoiacm.contentserver.model.MetadataClass;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.serial.gson.*;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmGsonHttpMessageConverter;
import org.bson.BSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.nio.charset.Charset;
import java.util.List;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
        super.configurePathMatch(configurer);
    }

    @Bean
    public HttpPutFormContentFilter httpPutFormContentFilter() {
        return new HttpPutFormContentFilter();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        super.configureContentNegotiation(configurer);
        configurer.favorPathExtension(false);
    }

//    private void configJacksonConverter(List<HttpMessageConverter<?>> converters) {
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        converter.setDefaultCharset(Charset.forName("UTF-8"));
//
//        ObjectMapper mapper = converter.getObjectMapper();
//        SimpleModule module = new SimpleModule();
//        module.addSerializer(BreakpointFile.class, new BreakpointFileJsonSerializer());
//        module.addSerializer(MetadataClass.class, new MetadataClassJsonSerializer());
//        module.addSerializer(MetadataAttr.class, new MetadataAttrJsonSerializer());
//        module.addSerializer(BSONObject.class, new BSONObjectJsonSerializer());
//        module.addSerializer(ScmBucket.class, new ScmBucketJsonSerializer());
//        module.addSerializer(ScmBucketAttachFailure.class, new ScmAttachFailureJsonSerializer());
//        mapper.registerModule(module);
//    }

    private void configScmGsonConverter(List<HttpMessageConverter<?>> converters) {
        ScmGsonHttpMessageConverter converter = ScmGsonHttpMessageConverter.start()
                .registerTypeAdapter(BreakpointFile.class, new BreakpointFileGsonTypeAdapter())
                .registerTypeAdapter(MetadataClass.class, new MetadataClassGsonTypeAdapter())
                .registerTypeAdapter(MetadataAttr.class, new MetadataAttrGsonTypeAdapter())
                .registerTypeAdapter(BSONObject.class, new BSONObjectGsonTypeAdapter())
                .registerTypeAdapter(ScmBucketAttachFailure.class,
                        new ScmAttachFailureTypeAdapter())
                .registerTypeAdapter(ScmBucket.class, new BucketGsonTypeAdapter())
                .registerTypeAdapter(ScmFileLocation.class, new ScmFileLocationGsonTypeAdapter())
                .build();

        converters.add(converter);

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setDefaultCharset(Charset.forName("UTF-8"));
        ObjectMapper mapper = jacksonConverter.getObjectMapper();
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        converters.add(jacksonConverter);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // converters for the following convert:
        // 1:controller parameter(@RequestBody), JSON => Object
        // 2:controller returned value, Object => JSON
        // 3:feign client interface returned value, JSON => Object
        super.configureMessageConverters(converters);
        configScmGsonConverter(converters);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // for controller parameter (@RquesParam), JSON => Object
        registry.addConverter(new BSONObjectGsonTypeAdapter());
    }
}