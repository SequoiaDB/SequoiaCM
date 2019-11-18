package com.sequoiacm.schedule.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.schedule.entity.ScheduleFullEntity;

import feign.Request.Options;

@Component
public class ScheduleClientFactory {
    private Map<String, ScheduleClient> nodeMapFeignClient = new ConcurrentHashMap<String, ScheduleClient>();
    private ScheduleFullEntityDecoder decoder = new ScheduleFullEntityDecoder();
    private ScheduleFeignExceptionConverter exceptionConverter = new ScheduleFeignExceptionConverter();

    @Autowired
    private ScmFeignClient scmFeignClient;

    public ScheduleClient getFeignClientByNodeUrl(String targetUrl) {
        if (nodeMapFeignClient.containsKey(targetUrl)) {
            return nodeMapFeignClient.get(targetUrl);
        } else {
            ScheduleClient client = scmFeignClient.builder()
                    .options(new Options(10 * 1000, 600 * 1000))
                    .exceptionConverter(exceptionConverter)
                    .typeDecoder(ScheduleFullEntity.class, decoder)
                    .instanceTarget(ScheduleClient.class, targetUrl);
            nodeMapFeignClient.put(targetUrl, client);
            return client;
        }
    }
}
