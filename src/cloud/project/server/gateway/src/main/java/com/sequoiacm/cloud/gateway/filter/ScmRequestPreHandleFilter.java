package com.sequoiacm.cloud.gateway.filter;

import com.sequoiacm.cloud.gateway.forward.decider.ForwardDecider;
import com.sequoiacm.cloud.gateway.forward.decider.Decision;
import com.sequoiacm.infrastructure.common.ScmRequestAttributeDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ScmRequestPreHandleFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(ScmRequestPreHandleFilter.class);

    private List<ForwardDecider> deciders;

    private HandlerExceptionResolver handlerExceptionResolver;

    @Autowired
    public ScmRequestPreHandleFilter(List<ForwardDecider> deciders,
            HandlerExceptionResolver handlerExceptionResolver) {
        logger.info("custom forward decider list: {}", deciders);
        if (deciders != null) {
            this.deciders = deciders;
        }
        else {
            this.deciders = Collections.emptyList();
        }
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            preHandle((HttpServletRequest) request);
        }
        catch (Exception e) {
            handlerExceptionResolver.resolveException((HttpServletRequest) request,
                    (HttpServletResponse) response, null, e);
            return;
        }
        chain.doFilter(request, response);

    }

    private void preHandle(HttpServletRequest request) {
        decideForwardService(request);
    }

    private void decideForwardService(HttpServletRequest request) {
        for (ForwardDecider decider : deciders) {
            Decision decision = decider.decide(request);
            if (decision != null) {
                request.setAttribute(ScmRequestAttributeDefine.FORWARD_SERVICE_ATTRIBUTE,
                        decision.getServiceName());
                request.setAttribute(ScmRequestAttributeDefine.FORWARD_DECISION,
                        decision);
                break;
            }
        }
    }
}
