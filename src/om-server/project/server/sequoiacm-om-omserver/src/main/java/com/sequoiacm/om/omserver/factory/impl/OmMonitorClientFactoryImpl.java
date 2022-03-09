package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.remote.OmMonitorFeignClient;
import com.sequoiacm.om.omserver.factory.OmMonitorClientFactory;
import feign.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OmMonitorClientFactoryImpl implements OmMonitorClientFactory {

    @Autowired
    private ScmFeignClient scmFeignClient;

    @Autowired
    private ScmOmServerConfig serverConfig;

    private Map<String, OmMonitorFeignClient> clientMap = new ConcurrentHashMap<>();

    @Override
    public OmMonitorFeignClient getClient(String managementUrl) {
        String url = managementUrl.replace("http://", "");
        OmMonitorFeignClient client = clientMap.get(url);
        if (client != null) {
            return client;
        }
        client = scmFeignClient.builder()
                .options(new Request.Options(serverConfig.getConnectTimeout(),
                        serverConfig.getReadTimeout()))
                .instanceTarget(OmMonitorFeignClient.class, url);
        clientMap.put(url, client);
        return client;
    }
}
