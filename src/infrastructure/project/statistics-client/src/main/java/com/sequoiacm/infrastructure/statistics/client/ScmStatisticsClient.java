package com.sequoiacm.infrastructure.statistics.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsRawData;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import javax.naming.Context;
import javax.naming.Name;
import java.util.Hashtable;
import java.util.List;

public class ScmStatisticsClient {
    private final ScmStatisticsFeignClient feign;

    public ScmStatisticsClient(ScmFeignClient feignClient) {
        final MappingJackson2HttpMessageConverter c = new MappingJackson2HttpMessageConverter(
                new ObjectMapper());
        ObjectFactory<HttpMessageConverters> converter = new ObjectFactory<HttpMessageConverters>() {
            @Override
            public HttpMessageConverters getObject() throws BeansException {
                return new HttpMessageConverters(c);
            }
        };
        this.feign = feignClient.builder().encoder(new SpringEncoder(converter))
                .serviceTarget(ScmStatisticsFeignClient.class, "admin-server");
    }

    public void reportRawData(String type, List<ScmStatisticsRawData> rawDataList)
            throws ScmFeignException {
        feign.reportRawData(type, rawDataList);
    }

}
