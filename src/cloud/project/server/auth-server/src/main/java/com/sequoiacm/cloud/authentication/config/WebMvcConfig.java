package com.sequoiacm.cloud.authentication.config;

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.session.data.sequoiadb.SequoiadbSession;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.cloud.authentication.controller.SequoiadbSessionGsonTypeAdapter;
import com.sequoiacm.cloud.authentication.controller.SequoiadbSessionJsonSerializer;
import com.sequoiacm.cloud.authentication.controller.SignatureInfoGsonTypeAdapter;
import com.sequoiacm.cloud.authentication.exception.RestException;
import com.sequoiacm.cloud.authentication.exception.RestExceptionJsonSerializer;
import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructrue.security.core.ScmPrivMeta;
import com.sequoiacm.infrastructrue.security.core.ScmPrivMetaSerializer;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilegeSerilizer;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmResourceSerializer;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmRoleJsonSerializer;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserJsonSerializer;
import com.sequoiacm.infrastructrue.security.core.serial.gson.AccesskeyInfoGsonTypeAdapter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmGsonHttpMessageConverter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmPrivMetaGsonTypeAdapter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmPrivilegeGsonTypeAdapter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmResourceGsonTypeAdapter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmRoleGsonTypeAdapter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmUserGsonTypeAdapter;

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

    private void configJacksonConverter(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setDefaultCharset(Charset.forName("UTF-8"));

        ObjectMapper mapper = converter.getObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ScmUser.class, new ScmUserJsonSerializer());
        module.addSerializer(ScmRole.class, new ScmRoleJsonSerializer());
        module.addSerializer(ScmPrivilege.class, new ScmPrivilegeSerilizer());
        module.addSerializer(ScmPrivMeta.class, new ScmPrivMetaSerializer());
        module.addSerializer(ScmResource.class, new ScmResourceSerializer());
        module.addSerializer(SequoiadbSession.class, new SequoiadbSessionJsonSerializer());
        module.addSerializer(RestException.class, new RestExceptionJsonSerializer());
        mapper.registerModule(module);

        converters.add(converter);
    }

    private void configScmGsonConverter(List<HttpMessageConverter<?>> converters) {
        ScmGsonHttpMessageConverter converter = ScmGsonHttpMessageConverter.start()
                .registerTypeAdapter(ScmUser.class, new ScmUserGsonTypeAdapter())
                .registerTypeAdapter(ScmRole.class, new ScmRoleGsonTypeAdapter())
                .registerTypeAdapter(ScmPrivilege.class, new ScmPrivilegeGsonTypeAdapter())
                .registerTypeAdapter(ScmPrivMeta.class, new ScmPrivMetaGsonTypeAdapter())
                .registerTypeAdapter(ScmResource.class, new ScmResourceGsonTypeAdapter())
                .registerTypeAdapter(SequoiadbSession.class, new SequoiadbSessionGsonTypeAdapter())
                .registerTypeAdapter(AccesskeyInfo.class, new AccesskeyInfoGsonTypeAdapter())
                // .registerTypeAdapter(RestException.class, new
                // RestExceptionGsonTypeAdapter())
                .build();
        // ScmGsonHttpMessageConverter converter =
        // ScmGsonHttpMessageConverter.start()
        // .registerTypeAdapter(ScmUser.class, new ScmUserGsonTypeAdapter())
        // .registerTypeAdapter(ScmRole.class, new ScmRoleGsonTypeAdapter())
        // .registerTypeAdapter(ScmPrivilege.class, new
        // ScmPrivilegeGsonTypeAdapter())
        // .registerTypeAdapter(ScmPrivMeta.class, new
        // ScmPrivMetaGsonTypeAdapter())
        // .registerTypeAdapter(ScmResource.class, new
        // ScmResourceGsonTypeAdapter())
        // .registerTypeAdapter(SequoiadbSession.class, new
        // SequoiadbSessionGsonTypeAdapter())
        // .registerTypeAdapter(RestException.class, new
        // RestExceptionGsonTypeAdapter())
        // .build();

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
        super.configureMessageConverters(converters);
        configScmGsonConverter(converters);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // for controller parameter (@RquesParam), JSON => Object
        registry.addConverter(new BSONObjectConverter());
        registry.addConverter(new SignatureInfoGsonTypeAdapter());
    }
}
