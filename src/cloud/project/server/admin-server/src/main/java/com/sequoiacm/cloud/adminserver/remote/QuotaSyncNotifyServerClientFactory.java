package com.sequoiacm.cloud.adminserver.remote;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class QuotaSyncNotifyServerClientFactory {

    @Autowired
    ScmFeignClient scmFeignClient;

    private Map<String, QuotaSyncNotifyServerClient> clients = new HashMap<>();

    public QuotaSyncNotifyServerClient getClient(String nodeUrl) {
        QuotaSyncNotifyServerClient client = clients.get(nodeUrl);
        if (client == null) {
            client = scmFeignClient.builder().instanceTarget(QuotaSyncNotifyServerClient.class,
                    nodeUrl);
            clients.put(nodeUrl, client);
        }
        return client;
    }
}
