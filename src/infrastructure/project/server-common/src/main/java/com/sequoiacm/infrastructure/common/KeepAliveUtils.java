package com.sequoiacm.infrastructure.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class KeepAliveUtils {

    public static final String REQUEST_KEEP_ALIVE_MARKER_ATTRIBUTE = KeepAliveUtils.class.getName()
            + ".REQUEST_KEEP_ALIVE_MARKER";

    public static void markRequestKeepAlive(HttpServletRequest request) {
        request.setAttribute(REQUEST_KEEP_ALIVE_MARKER_ATTRIBUTE, "true");
    }

    public static boolean isCurrentRequestKeepAlive() {
        HttpServletRequest request = null;
        try {
            request = getCurrentRequest();
        }
        catch (Exception e) {
            // 抛出异常说明当前线程不是 web 请求线程
            return false;
        }

        if (request == null) {
            return false;
        }
        return Boolean.parseBoolean(
                String.valueOf(request.getAttribute(REQUEST_KEEP_ALIVE_MARKER_ATTRIBUTE)));
    }

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes();
        return requestAttributes.getRequest();
    }

}
