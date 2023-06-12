package com.sequoiacm.infrastructure.security.auth;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserJsonDeserializer;
import com.sequoiacm.infrastructure.common.SecurityRestField;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "scm.authFilter", name = "headerFirst", havingValue = "false", matchIfMissing = false)
public class ScmAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuthenticationFilter.class);

    private AntPathMatcher matcher = new AntPathMatcher();

    private ScmSessionMgr sessionMgr;

    private ScmFeignClient feignClient;

    @Value("${zuul.routes.login.path:/login}")
    private String loginPath = "/login";

    @Value("${zuul.routes.v2Login.path:/v2/localLogin}")
    private String v2LocalLoginPath = "/v2/localLogin";

    @Value("${zuul.routes.logout.path:/logout}")
    private String logoutPath = "/logout";

    @Autowired
    public ScmAuthenticationFilter(ScmFeignClient feignClient) {
        this.feignClient = feignClient;
        this.sessionMgr = new ScmSessionMgr(feignClient);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String sessionId = request.getHeader(SecurityRestField.SESSION_ATTRIBUTE);
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

            request.setAttribute(SecurityRestField.USER_ATTRIBUTE, userDetails);
            if (!response.containsHeader(SecurityRestField.SESSION_ATTRIBUTE)) {
                response.setHeader(SecurityRestField.SESSION_ATTRIBUTE, sessionId);
            }
            request.setAttribute(SecurityRestField.USER_INFO_WRAPPER,
                    new ScmUserWrapper(user, userDetails));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
            authentication.eraseCredentials();
            UsernamePasswordAuthenticationToken newAuth = ScmAuthenticationHelper
                    .newAuthWithActuatorRole(authentication);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }else if(StringUtils.hasText(sessionId) && isLogoutReq) {
            sessionMgr.markSessionLogout(sessionId);
        }

        filterChain.doFilter(request, response);

        // cache user info when processing login requests
        if (sessionMgr instanceof ScmSessionMgrWithSessionCache
                && (isLoginRequest(request) || isV2LocalLoginRequest(request))) {
            String loginSessionId = response.getHeader(SecurityRestField.SESSION_ATTRIBUTE);
            String userJson = response.getHeader(SecurityRestField.USER_DETAILS);
            if (loginSessionId == null || userJson == null) {
                return;
            }
            BSONObject bsonObject = (BSONObject) JSON.parse(userJson);
            ScmUser scmUser = ScmUserJsonDeserializer.deserialize(bsonObject);
            ScmUserWrapper scmUserWrapper = new ScmUserWrapper(scmUser, bsonObject.toString());
            ((ScmSessionMgrWithSessionCache) sessionMgr).putCache(loginSessionId, scmUserWrapper);
        }

    }

    private boolean isLoginRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matcher.match(loginPath, uri);
    }

    private boolean isV2LocalLoginRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matcher.match(v2LocalLoginPath, uri);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matcher.match(logoutPath, uri);
    }

    public void enableCache(long cacheTimeToLive) {
        if(this.sessionMgr != null) {
            this.sessionMgr.close();
        }
        this.sessionMgr = new ScmSessionMgrWithSessionCache(feignClient, cacheTimeToLive);
    }

    public void removeCache(String userName) {
        try {
            if (sessionMgr instanceof ScmSessionMgrWithSessionCache) {
                ((ScmSessionMgrWithSessionCache) sessionMgr).removeUserCache(userName);
            }
        }
        catch (Exception e) {
            logger.warn("Failed to remove cache, user = " + userName, e);
        }
    }

    public void disableCache() {
        if(this.sessionMgr != null) {
            this.sessionMgr.close();
        }
        this.sessionMgr = new ScmSessionMgr(feignClient);
    }
}
