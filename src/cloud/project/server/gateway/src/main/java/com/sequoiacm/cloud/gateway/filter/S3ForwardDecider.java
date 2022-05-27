package com.sequoiacm.cloud.gateway.filter;

import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@EnableScmServiceDiscoveryClient
@Component
public class S3ForwardDecider implements CustomForwardDecider {

    @Autowired
    private ScmServiceDiscoveryClient discoveryClient;

    @Override
    public Decision decide(HttpServletRequest req) {
        String s3ServiceName = getS3ServiceName(req);
        if (s3ServiceName == null) {
            return Decision.shouldNotForward();
        }
        String uri = req.getRequestURI();
        String targetApi = uri.substring(("/" + s3ServiceName).length());
        return Decision.shouldForward(s3ServiceName, targetApi, null, false);
    }

    private String getS3ServiceName(HttpServletRequest clientReq) {
        String url = clientReq.getRequestURI();
        if (url == null || !url.startsWith("/")) {
            return null;
        }
        // url = /serviceName or /serviceName/XXX
        String serviceName = url.substring(1).trim();
        if (serviceName.length() <= 0) {
            return null;
        }

        if (serviceName.contains("/")) {
            serviceName = serviceName.substring(0, serviceName.indexOf("/"));
        }

        List<ScmServiceInstance> instance = discoveryClient.getInstances(serviceName);
        if (instance == null || instance.isEmpty()) {
            return null;
        }
        if (instance.get(0).getMetadata() == null) {
            return null;
        }

        String s3Flag = instance.get(0).getMetadata().get("isS3Server");
        if (s3Flag == null) {
            return null;
        }
        if (!Boolean.parseBoolean(s3Flag)) {
            return null;
        }
        return serviceName;
    }

    @Override
    public String toString() {
        return "S3ForwardDecider";
    }
}
