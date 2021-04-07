package com.sequoiacm.cloud.adminserver;

import java.nio.charset.Charset;
import java.util.List;

import com.sequoiacm.cloud.adminserver.common.FileStatisticsDataForClientSerializer;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataQueryCondition;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.cloud.adminserver.common.BSONObjectConverter;
import com.sequoiacm.cloud.adminserver.common.FileDeltaInfoJsonSerializer;
import com.sequoiacm.cloud.adminserver.common.TrafficInfoJsonSerializer;
import com.sequoiacm.infrastructure.feign.BSONObjectJsonDeserializer;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

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
        module.addDeserializer(BSONObject.class, new BSONObjectJsonDeserializer<BSONObject>());

        module.addSerializer(new TrafficInfoJsonSerializer());
        module.addSerializer(new FileDeltaInfoJsonSerializer());
        module.addSerializer(new FileStatisticsDataForClientSerializer());
        mapper.registerModule(module);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new BSONObjectConverter());
        registry.addConverter(new Converter<String, FileStatisticsDataQueryCondition>() {
            @Override
            public FileStatisticsDataQueryCondition convert(String source) {
                BSONObject bson = (BSONObject) JSON.parse(source);
                FileStatisticsDataQueryCondition ret = new FileStatisticsDataQueryCondition();
                ret.setBegin(BsonUtils.getString(bson, ScmStatisticsDefine.REST_FIELD_BEGIN));
                ret.setEnd(BsonUtils.getString(bson, ScmStatisticsDefine.REST_FIELD_END));
                String timeAccuracyStr = BsonUtils.getString(bson,
                        ScmStatisticsDefine.REST_FIELD_TIME_ACCURACY);
                if (timeAccuracyStr != null) {
                    ret.setTimeAccuracy(ScmTimeAccuracy.valueOf(timeAccuracyStr));
                }
                ret.setUser(BsonUtils.getString(bson, ScmStatisticsDefine.REST_FIELD_USER));
                ret.setWorkspace(
                        BsonUtils.getString(bson, ScmStatisticsDefine.REST_FIELD_WORKSPACE));
                return ret;
            }
        });
    }
}