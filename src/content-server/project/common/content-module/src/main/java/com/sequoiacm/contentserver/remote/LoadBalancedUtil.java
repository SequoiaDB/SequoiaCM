package com.sequoiacm.contentserver.remote;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;

@Component
public class LoadBalancedUtil {

    private static LoadBalancerClient loadBlancerClient;

    @Autowired(required = true)
    public void setLoadBlancerClient(LoadBalancerClient loadBlancerClient) {
        LoadBalancedUtil.loadBlancerClient = loadBlancerClient;
    }

    public static String choose(String serviceName) throws ScmServerException {
        ServiceInstance instance = chooseInstance(serviceName);
        return instance.getHost() + ":" + instance.getPort();

    }

    public static ServiceInstance chooseInstance(String serviceName) throws ScmServerException {
        ServiceInstance instance = loadBlancerClient.choose(serviceName.toLowerCase());
        if (instance == null) {
            throw new ScmSystemException("no instance fo this service:serviceName" + serviceName);
        }
        return instance;
    }

}
