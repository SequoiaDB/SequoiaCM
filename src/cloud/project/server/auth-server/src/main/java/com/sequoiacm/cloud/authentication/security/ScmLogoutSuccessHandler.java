package com.sequoiacm.cloud.authentication.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;

import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;

public class ScmLogoutSuccessHandler implements LogoutSuccessHandler {
    private static Logger logger = LoggerFactory.getLogger(ScmLogoutSuccessHandler.class);

    private final HttpStatus httpStatusToReturn;

    private ScmAudit audit;

    public ScmLogoutSuccessHandler(ScmAudit audit) {
        this.httpStatusToReturn = HttpStatus.OK;
        this.audit = audit;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String sessionId = request.getHeader("x-auth-token");
        if (!StringUtils.hasText(sessionId)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "No session id");
            return;
        }

        if (authentication == null) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Session does not exist: " + sessionId);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("User {} logout, session: {}",
                    authentication.getPrincipal(), sessionId);
        }

        audit.info(ScmAuditType.LOGOUT, authentication, null, 0, "logout, sessionId=" + sessionId);
        response.setStatus(this.httpStatusToReturn.value());
        response.getWriter().flush();
    }
}
