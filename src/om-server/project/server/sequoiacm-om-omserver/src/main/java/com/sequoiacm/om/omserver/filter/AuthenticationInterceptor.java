package com.sequoiacm.om.omserver.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.session.ScmOmSessionMgr;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private ScmOmSessionMgr sessionMgr;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        String sessionId = request.getHeader(RestParamDefine.X_AUTH_TOKEN);
        if (sessionId != null) {
            if (sessionMgr.getSession(sessionId) == null) {
                response.sendError(401, "session not exist:sessionId=" + sessionId);
                return false;
            }
            return true;
        }

        // TODO: response.sendRedirect("/");
        response.sendError(401, "session not exist:sessionId=" + sessionId);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {

    }

}
