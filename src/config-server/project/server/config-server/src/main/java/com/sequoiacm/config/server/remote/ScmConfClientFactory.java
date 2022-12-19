package com.sequoiacm.config.server.remote;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmServiceUpdateConfigResult;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;

import feign.Response;
import feign.codec.Decoder;

@Component
public class ScmConfClientFactory {
    @Autowired
    private ScmFeignClient scmFeignClient;


    private Map<String, ScmConfClient> urlMapClient = new ConcurrentHashMap<>();
    private Map<String, ScmConfClient> serviceMapClient = new ConcurrentHashMap<>();
    private ScmConfServerExceptionConvertor exceptionConvertor = new ScmConfServerExceptionConvertor();

    public ScmConfClient getClient(String nodeUrl) {
        if (urlMapClient.containsKey(nodeUrl)) {
            return urlMapClient.get(nodeUrl);
        }

        ScmConfClient client = scmFeignClient.builder().exceptionConverter(exceptionConvertor)
                .typeDecoder(ScmServiceUpdateConfigResult.class,
                        new ScmServiceUpdateConfResultDecoder())
                .instanceTarget(ScmConfClient.class, nodeUrl);
        urlMapClient.put(nodeUrl, client);
        return client;
    }

    public ScmConfClient getClientByService(String serviceName) {
        if (serviceMapClient.containsKey(serviceName)) {
            return serviceMapClient.get(serviceName);
        }

        ScmConfClient client = scmFeignClient.builder().exceptionConverter(exceptionConvertor)
                .typeDecoder(ScmServiceUpdateConfigResult.class,
                        new ScmServiceUpdateConfResultDecoder())
                .serviceTarget(ScmConfClient.class, serviceName.toLowerCase());
        serviceMapClient.put(serviceName, client);
        return client;
    }

}

class ScmServiceUpdateConfResultDecoder implements Decoder {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object decode(Response response, Type type) throws IOException {
        ScmServiceUpdateConfigResult ret = new ScmServiceUpdateConfigResult();
        Set<String> rebootConf = Collections.emptySet();
        Map<String, String> adjustConf = Collections.emptyMap();
        Collection<String> rebootConfHeader = response.headers()
                .get(ScmRestArgDefine.CONF_PROPS_REBOOT_CONF);
        if (rebootConfHeader != null && !rebootConfHeader.isEmpty()) {
            rebootConf = objectMapper.readValue(rebootConfHeader.iterator().next(), Set.class);
        }

        Collection<String> adjustConfHeader = response.headers()
                .get(ScmRestArgDefine.CONF_PROPS_ADJUST_CONF);
        if (adjustConfHeader != null && !adjustConfHeader.isEmpty()) {
            adjustConf = objectMapper.readValue(adjustConfHeader.iterator().next(), Map.class);
        }

        ret.setAdjustedConf(adjustConf);
        ret.setRebootConf(rebootConf);
        return ret;
    }
}
