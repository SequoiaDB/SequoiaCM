package com.sequoiacm.cloud.gateway.filter;

import com.sequoiacm.cloud.gateway.service.UploadForwardServiceImpl;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.monitor.ReqRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@EnableScmServiceDiscoveryClient
@Component
public class CustomForwardFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CustomForwardFilter.class);

    @Autowired
    private UploadForwardServiceImpl service;

    private List<CustomForwardDecider> deciders;

    @Autowired
    public CustomForwardFilter(List<CustomForwardDecider> deciders) {
        if (deciders != null) {
            this.deciders = deciders;
        }
        else {
            this.deciders = Collections.emptyList();
        }
        logger.info("custom forward decider list: {}", deciders);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest clientReq, HttpServletResponse clientResp,
            FilterChain filterChain) throws ServletException, IOException {

        Decision decision = null;
        for (CustomForwardDecider decider : deciders) {
            decision = decider.decide(clientReq);
            if (decision.isCustomForward()) {
                break;
            }
        }
        if (decision == null || !decision.isCustomForward()) {
            filterChain.doFilter(clientReq, clientResp);
            return;
        }

        long before = System.currentTimeMillis();
        try {
            logger.debug("custom forward req:service={}, targetApi={}", decision.getServiceName(),
                    decision.getTargetApi());
            service.forward(decision.getServiceName(), decision.getTargetApi(), clientReq,
                    clientResp, decision.isChunkedForward());
        }
        catch (Exception e) {
            logger.error("failed to forward s3 request: serviceName={}, reqUrl={}, queryParam={}",
                    decision.getServiceName(), clientReq.getRequestURI(),
                    clientReq.getParameterMap(), e);
            clientResp.sendError(500, e.getMessage());
        }
        finally {
            ReqRecorder.getInstance().addRecord(System.currentTimeMillis() - before);
        }
    }
}
