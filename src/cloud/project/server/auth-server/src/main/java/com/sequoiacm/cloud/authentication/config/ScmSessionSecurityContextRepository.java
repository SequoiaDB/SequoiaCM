package com.sequoiacm.cloud.authentication.config;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.infrastructure.common.SecurityRestField;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserJsonDeserializer;

public class ScmSessionSecurityContextRepository extends HttpSessionSecurityContextRepository {

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext securityContext = super.loadContext(requestResponseHolder);
        Authentication auth = securityContext.getAuthentication();
        if (auth != null) {
            ScmUser userDetailInSession = (ScmUser) auth.getPrincipal();
            ScmUser userDetailInRequest = parseScmUserDetail(requestResponseHolder.getRequest());
            if (userDetailInRequest != null
                    && !isEqualUserDetail(userDetailInSession, userDetailInRequest)) {
                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                        userDetailInRequest, auth.getCredentials(),
                        userDetailInRequest.getAuthorities());
                newAuth.setDetails(auth.getDetails());
                securityContext.setAuthentication(newAuth);
            }
        }
        return securityContext;
    }

    private ScmUser parseScmUserDetail(HttpServletRequest request) {
        String userDetails = request.getHeader(SecurityRestField.USER_ATTRIBUTE);
        if (userDetails == null) {
            return null;
        }
        BSONObject userDetailsObj = (BSONObject) JSON.parse(userDetails);
        return ScmUserJsonDeserializer.deserialize(userDetailsObj);
    }

    private boolean isEqualUserDetail(ScmUser user1, ScmUser user2) {
        if (user1 == null && user2 == null) {
            return true;
        }
        if (user1 == null || user2 == null) {
            return false;
        }
        return user1.equals(user2);
    }
}
