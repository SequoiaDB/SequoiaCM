package com.sequoiacm.cloud.gateway.forward;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.cloud.gateway.forward.decider.Decision;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmRequestAttributeDefine;
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

    private static final String SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE = CustomForwardHandlerMapping.class
            .getName() + ".MAPPING.MARKER";
    private static final Logger logger = LoggerFactory.getLogger(CustomForwardHandlerMapping.class);
    private final HandlerMethod handlerMethod;
    @Autowired
    private CustomForwarder forwarder;

    public CustomForwardHandlerMapping() throws NoSuchMethodException {
        Method method = CustomForwardHandlerMapping.class.getMethod("forward",
                HttpServletRequest.class, HttpServletResponse.class);
        handlerMethod = new HandlerMethod(this, method);
    }

    @Override
    protected Object getHandlerInternal(HttpServletRequest request) throws ScmServerException {
        // 该请求被本 Mapping 处理过时，设置这个属性
        if (request.getAttribute(SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE) != null) {
            // 当请求处理失败时，tomcat 会修改请求的 url 为 /error 令 spring 重新遍历 handler mapping 获取 error
            // handler
            // 这个 handler mapping 不能处理 /error，返回 null 让 spring 找下一个 handler mapping
            return null;
        }
        request.setAttribute(SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE,
                SCM_CUSTOM_MAPPING_MARKER_ATTRIBUTE);

        Decision decision = (Decision) request
                .getAttribute(ScmRequestAttributeDefine.FORWARD_DECISION);
        if (decision == null) {
            // should never happen
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "forward decision not found, requestPath=" + request.getRequestURI()
                            + ", method=" + request.getMethod());
        }
        if (!decision.isCustomForward()) {
            return null;
        }

        request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/" + decision.getServiceName() + "/**");
        return handlerMethod;

    }

    public void forward(HttpServletRequest clientReq, HttpServletResponse clientResp)
            throws Exception {
        Decision decision = (Decision) clientReq
                .getAttribute(ScmRequestAttributeDefine.FORWARD_DECISION);
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
