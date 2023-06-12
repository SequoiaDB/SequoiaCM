package com.sequoiacm.config.server;

import java.nio.charset.Charset;
import java.util.List;

import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.config.server.common.BSONObjectConverter;
import com.sequoiacm.config.server.common.ConfigSerializer;
import com.sequoiacm.config.server.common.ScmSubscriberSerializer;
import com.sequoiacm.config.server.common.VersionSerializer;
import com.sequoiacm.config.server.module.ScmConfPropsParam;
import com.sequoiacm.config.server.module.ScmConfPropsParamGsonTypeAdapter;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSet;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSetGsonTypeAdapter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmGsonHttpMessageConverter;
import com.sequoiacm.infrastructure.feign.BSONObjectJsonDeserializer;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    @Bean
    public HttpPutFormContentFilter httpPutFormContentFilter() {
        return new HttpPutFormContentFilter();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        super.configureContentNegotiation(configurer);
        configurer.favorPathExtension(false);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);

        ScmGsonHttpMessageConverter gonMessageConverter = ScmGsonHttpMessageConverter.start()
                .registerTypeAdapter(ScmConfPropsParam.class,
                        new ScmConfPropsParamGsonTypeAdapter())
                .registerTypeAdapter(ScmUpdateConfPropsResultSet.class, new ScmUpdateConfPropsResultSetGsonTypeAdapter())
                .build();
        converters.add(gonMessageConverter);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setDefaultCharset(Charset.forName("UTF-8"));
        converters.add(converter);

        ObjectMapper mapper = converter.getObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(BSONObject.class, new BSONObjectJsonDeserializer<BSONObject>());
        module.addSerializer(new ScmSubscriberSerializer());
        module.addSerializer(new ConfigSerializer(configEntityTranslator));
        module.addSerializer(new VersionSerializer());
        mapper.registerModule(module);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new BSONObjectConverter());
    }
}