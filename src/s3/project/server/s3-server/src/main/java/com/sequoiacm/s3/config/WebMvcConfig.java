package com.sequoiacm.s3.config;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequoiacm.s3.model.CreateBucketConfiguration;

@Configuration
@EnableWebMvc
public class WebMvcConfig extends WebMvcConfigurerAdapter {

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

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setDefaultCharset(Charset.forName("UTF-8"));
        converters.add(converter);

        ObjectMapper mapper = converter.getObjectMapper();
        SimpleModule module = new SimpleModule();
        // module.addSerializer(Error.class, new ErrorSerializer());
        // module.addSerializer(Error.class, new ErrorSerializer());
        mapper.registerModule(module);

        XmlMapper mapperXml = new XmlMapper();
        SimpleModule moduleXml = new SimpleModule();
        // moduleXml.addSerializer(Error.class, new ErrorSerializer());
        mapperXml.registerModule(moduleXml);

        MappingJackson2XmlHttpMessageConverter converterXml = new MyXmlConverter(mapperXml);
        converterXml.setDefaultCharset(Charset.forName("UTF-8"));
        converters.add(converterXml);

    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
        argumentResolvers.add(new ScmSessionArgResolver());
        argumentResolvers.add(new ObjMetaArgResolver());
        argumentResolvers.add(new ObjMatcherArgResolver());
    }

}

class MyXmlConverter extends MappingJackson2XmlHttpMessageConverter {

    public MyXmlConverter(ObjectMapper objectMapper) {
        super(objectMapper);
        List<MediaType> types = super.getSupportedMediaTypes();
        List<MediaType> myTypes = new ArrayList<>();
        myTypes.add(MediaType.parseMediaType("application/octet-stream;charset=UTF-8"));
        myTypes.addAll(types);
        setSupportedMediaTypes(myTypes);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        boolean canRead = super.canRead(clazz, mediaType);
        if (!canRead) {
            return clazz.equals(CreateBucketConfiguration.class);
        }
        return canRead;
    }

}
