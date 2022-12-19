package com.sequoiacm.config.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResult;
import com.sequoiacm.config.server.remote.ScmConfClient;
import com.sequoiacm.config.server.remote.ScmConfClientFactory;
import com.sequoiacm.infrastructure.config.core.common.ScmServiceUpdateConfigResult;

@Component
public class AsyncUpdateConfPropsExecutor {
    private static final Logger logger = LoggerFactory
            .getLogger(AsyncUpdateConfPropsExecutor.class);

    @Autowired
    private ScmConfClientFactory feignClientFactory;

    private Gson gson = new Gson();

    @Async
    public Future<List<ScmUpdateConfPropsResult>> updateConfProp(List<ServiceInstance> instances,
            Map<String, String> updateProps, List<String> deleteProps, boolean acceptUnknownProps) {
        List<ScmUpdateConfPropsResult> resList = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            String instanceUrl = instance.getHost() + ":" + instance.getPort();
            try {
                ScmConfClient feignClient = feignClientFactory.getClient(instanceUrl);
                ScmServiceUpdateConfigResult res = feignClient.updateConfigProps(
                        gson.toJson(updateProps), gson.toJson(deleteProps),
                        acceptUnknownProps);
                resList.add(new ScmUpdateConfPropsResult(instance.getServiceId(), instanceUrl,
                        res.getRebootConf(), res.getAdjustedConf()));
            }
            catch (Exception e) {
                logger.warn(
                        "failed to update conf properties on the server:serverUrl={},serviceName={}",
                        instanceUrl, instance.getServiceId(), e);
                resList.add(new ScmUpdateConfPropsResult(instance.getServiceId(), instanceUrl,
                        e.getMessage()));
            }
        }
        return new AsyncResult<List<ScmUpdateConfPropsResult>>(resList);
    }
}
