package com.sequoiacm.clean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.DataInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteClientMgr {

    ScmFeignUtil feignUtil = new ScmFeignUtil();
    DataInfoDecoder dataInfoDecoder = new DataInfoDecoder();
    Map<String, ContentServerClient> clients = new ConcurrentHashMap<>();

    private static final ContentServerFeignExceptionConverter exceptionConverter = new ContentServerFeignExceptionConverter();

    private static ObjectMapper mapper = new ObjectMapper();
    static {
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
    }

    public ContentServerClient getClient(String url) {
        ContentServerClient client = clients.get(url);
        if (client == null) {
            client = feignUtil.builder().typeDecoder(DataInfo.class, dataInfoDecoder)
                    .exceptionConverter(exceptionConverter).loggerLevel(feign.Logger.Level.BASIC)
                    .objectMapper(mapper).instanceTarget(ContentServerClient.class, url);
            clients.put(url, client);
        }
        return client;
    }
}
