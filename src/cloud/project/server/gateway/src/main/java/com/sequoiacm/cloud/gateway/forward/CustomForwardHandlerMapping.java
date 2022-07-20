package com.sequoiacm.cloud.gateway.forward;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.cloud.gateway.forward.decider.CustomForwardDecider;
import com.sequoiacm.cloud.gateway.forward.decider.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import com.sequoiacm.infrastructure.monitor.ReqRecorder;

@Component
public class CustomForwardHandlerMapping extends AbstractHandlerMapping {
    private static final String SCM_CUSTOM_FORWARD_DECISION_ATTRIBUTE = CustomForwardHandlerMapping.class
            .getName() + ".DECISION";
    private static final String SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE = CustomForwardHandlerMapping.class
            .getName() + ".MAPPING.MARKER";
    private static final Logger logger = LoggerFactory.getLogger(CustomForwardHandlerMapping.class);
    private final List<CustomForwardDecider> deciders;
    private final HandlerMethod handlerMethod;
    @Autowired
    private CustomForwarder forwarder;

    @Autowired
    public CustomForwardHandlerMapping(List<CustomForwardDecider> deciders)
            throws NoSuchMethodException {
        if (deciders != null) {
            this.deciders = deciders;
        }
        else {
            this.deciders = Collections.emptyList();
        }
        logger.info("custom forward decider list: {}", deciders);
        Method method = CustomForwardHandlerMapping.class.getMethod("forward",
                HttpServletRequest.class, HttpServletResponse.class);
        handlerMethod = new HandlerMethod(this, method);
    }

    @Override
    protected Object getHandlerInternal(HttpServletRequest request) {
        // 该请求被本 Mapping 处理过时，设置这个属性
        if (request.getAttribute(SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE) != null) {
            // 当请求处理失败时，tomcat 会修改请求的 url 为 /error 令 spring 重新遍历 handler mapping 获取 error
            // handler
            // 这个 handler mapping 不能处理 /error，返回 null 让 spring 找下一个 handler mapping
            return null;
        }
        request.setAttribute(SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE,
                SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE);

        Decision decision = null;
        for (CustomForwardDecider decider : deciders) {
            decision = decider.decide(request);
            if (decision.isCustomForward()) {
                break;
            }
        }
        if (decision == null || !decision.isCustomForward()) {
            return null;
        }

        request.setAttribute(SCM_CUSTOM_FORWARD_DECISION_ATTRIBUTE, decision);
        request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/" + decision.getServiceName() + "/**");
        return handlerMethod;

    }

    public void forward(HttpServletRequest clientReq, HttpServletResponse clientResp)
            throws Exception {
        Decision decision = (Decision) clientReq.getAttribute(SCM_CUSTOM_FORWARD_DECISION_ATTRIBUTE);
        long before = System.currentTimeMillis();
        try {
            logger.debug("custom forward req:service={}, targetApi={}", decision.getServiceName(),
                    decision.getTargetApi());
            forwarder.forward(decision.getServiceName(), decision.getTargetApi(), clientReq,
                    clientResp, decision.getDefaultContentType(), decision.isChunkedForward(),
                    decision.isSetFrowardPrefix());
        }
        catch (Exception e) {
            logger.error("failed to forward request: serviceName={}, reqUrl={}, queryParam={}",
                    decision.getServiceName(), clientReq.getRequestURI(),
                    clientReq.getQueryString());
            throw e;
        }
        finally {
            ReqRecorder.getInstance().addRecord(System.currentTimeMillis() - before);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
