package com.sequoiacm.om.omserver.factory.impl;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.factory.ScmServiceCenterClientFactory;
import com.sequoiacm.om.omserver.remote.ScmServiceCenterFeignClient;
import feign.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScmServiceCenterClientFactoryImpl implements ScmServiceCenterClientFactory {

    private Map<String, ScmServiceCenterFeignClient> clientMap = new ConcurrentHashMap<>();

    @Autowired
    private ScmFeignClient scmFeignClient;

    @Autowired
    private ScmOmServerConfig serverConfig;

    @Override
    public ScmServiceCenterFeignClient getClient(String url) {
        url = url.replace("http://", "");
        ScmServiceCenterFeignClient client = clientMap.get(url);
        if (client != null) {
            return client;
        }
        client = scmFeignClient.builder()
                .options(new Request.Options(serverConfig.getConnectTimeout(),
                        serverConfig.getReadTimeout()))
                .instanceTarget(ScmServiceCenterFeignClient.class, url);
        clientMap.put(url, client);
        return client;
    }
}
