package com.sequoiacm.cloud.authentication.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;

public class ScmAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuthenticationSuccessHandler.class);

    private final HttpStatus httpStatusToReturn;

    private ScmAudit audit;

    public ScmAuthenticationSuccessHandler(ScmAudit audit) {
        this.httpStatusToReturn = HttpStatus.OK;
        this.audit = audit;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug("User {} login, session: {}",
                    authentication.getPrincipal(), request.getSession().getId());
        }
        audit.info(ScmAuditType.LOGIN, authentication, null, 0, "login, sessionId=" + request.getSession().getId());
        response.setStatus(this.httpStatusToReturn.value());
        response.getWriter().flush();
    }
}
