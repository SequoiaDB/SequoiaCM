package com.sequoiacm.infrastructure.security.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.util.JSON;
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
import com.sequoiacm.infrastructrue.security.core.ScmUserJsonDeserializer;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;

@Component
@ConditionalOnProperty(prefix = "scm.authFilter", name = "headerFirst", havingValue = "true", matchIfMissing = true)
public class ScmAuthFromHeaderFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuthFromHeaderFilter.class);

    private AntPathMatcher matcher = new AntPathMatcher();
    private ScmConcurrentLRUMap<String, ScmUser> userObjectCache;
    private ScmSessionMgr sessionMgr;

    @Autowired
    public ScmAuthFromHeaderFilter(ScmFeignClient feignClient) {
        this.sessionMgr = new ScmSessionMgr(feignClient);
        this.userObjectCache = new ScmConcurrentLRUMap<>(200);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String sessionId = request.getHeader(RestField.SESSION_ATTRIBUTE);
        boolean isLogoutReq = isLogoutRequest(request);
        if (StringUtils.hasText(sessionId) && !isLogoutReq) {
            ScmUser user;
            String userDetails = request.getHeader(RestField.USER_ATTRIBUTE);
            if (userDetails != null) {
                user = userObjectCache.get(userDetails);
                if (user == null) {
                    BSONObject userDetailsObj = (BSONObject) JSON.parse(userDetails);
                    user = ScmUserJsonDeserializer.deserialize(userDetailsObj);
                    userObjectCache.put(userDetails, user);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Get user from header user details");
                }
            }
            else {
                logger.warn("user header not found, get user from auth server");
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
            }
            request.setAttribute(RestField.USER_ATTRIBUTE, userDetails);
            request.setAttribute(RestField.USER_INFO_WRAPPER,
                    new ScmUserWrapper(user, userDetails));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            authentication.eraseCredentials();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        else if (StringUtils.hasText(sessionId) && isLogoutReq) {
            sessionMgr.markSessionLogout(sessionId);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matcher.match("/**/logout", uri);
    }
}
