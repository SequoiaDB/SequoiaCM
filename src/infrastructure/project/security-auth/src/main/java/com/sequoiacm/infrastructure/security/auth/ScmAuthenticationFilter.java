package com.sequoiacm.infrastructure.security.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;

@Component
@ConditionalOnProperty(prefix = "scm.authFilter", name = "headerFirst", havingValue = "false", matchIfMissing = false)
public class ScmAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuthenticationFilter.class);

    private AntPathMatcher matcher = new AntPathMatcher();

    private ScmSessionMgr sessionMgr;

    private ScmFeignClient feignClient;

    @Autowired
    public ScmAuthenticationFilter(ScmFeignClient feignClient) {
        this.feignClient = feignClient;
        this.sessionMgr = new ScmSessionMgr(feignClient);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String sessionId = request.getHeader(RestField.SESSION_ATTRIBUTE);
        boolean isLogoutReq = isLogoutRequest(request);
        if (StringUtils.hasText(sessionId) && !isLogoutReq) {
            ScmUser user;
            String userDetails;
            try {
                ScmUserWrapper userWrapper = sessionMgr.getUserBySessionId(sessionId);
                user = userWrapper.getUser();
                userDetails = userWrapper.getUserJSON();
            }
            catch (ScmFeignException e) {
                String msg = String.format(
                        "Failed to get session from auth-server for %s@%s, sessionId=%s",
                        request.getMethod(), request.getRequestURI(), sessionId);
                logger.error(msg, e);
                response.sendError(e.getStatus(), e.getMessage());
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.info("Get user from session user details");
            }

            request.setAttribute(RestField.USER_ATTRIBUTE, userDetails);
            if (!response.containsHeader(RestField.SESSION_ATTRIBUTE)) {
                response.setHeader(RestField.SESSION_ATTRIBUTE, sessionId);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
            authentication.eraseCredentials();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }else if(StringUtils.hasText(sessionId) && isLogoutReq) {
            sessionMgr.markSessionLogout(sessionId);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matcher.match("/**/logout", uri);
    }

    public void enableCache(long cacheTimeToLive) {
        if(this.sessionMgr != null) {
            this.sessionMgr.close();
        }
        this.sessionMgr = new ScmSessionMgrWithSessionCache(feignClient, cacheTimeToLive);
    }

    public void disableCache() {
        if(this.sessionMgr != null) {
            this.sessionMgr.close();
        }
        this.sessionMgr = new ScmSessionMgr(feignClient);
    }
}
