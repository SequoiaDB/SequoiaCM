package com.sequoiacm.content.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.content.client.remote.ContentserverFeign;
import com.sequoiacm.content.client.remote.FeignExceptionConverter;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;

@Component
public class ContentserverClientMgr {
    private ScmFeignClient feignClient;
    private Map<String, ContentserverClient> clients = new ConcurrentHashMap<>();

    @Autowired
    public ContentserverClientMgr(ScmFeignClient feign) {
        this.feignClient = feign;
    }

    public ContentserverClient getClient(String siteName) {
        siteName = siteName.toLowerCase();
        ContentserverClient client = clients.get(siteName);
        if (client == null) {
            ContentserverFeign contentserverFiegn = feignClient.builder()
                    .exceptionConverter(new FeignExceptionConverter())
                    .serviceTarget(ContentserverFeign.class, siteName);
            client = new ContentserverClient(siteName, contentserverFiegn);
            clients.put(siteName, client);
        }
        return client;
    }
}
