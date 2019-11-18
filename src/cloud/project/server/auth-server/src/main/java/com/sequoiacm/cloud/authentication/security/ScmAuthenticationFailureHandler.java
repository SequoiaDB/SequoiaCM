package com.sequoiacm.cloud.authentication.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ScmAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private static Logger logger = LoggerFactory.getLogger(ScmAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug("User {} login failure, exception: {}",
                    request.getParameter("username"), exception);
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
    }
}
