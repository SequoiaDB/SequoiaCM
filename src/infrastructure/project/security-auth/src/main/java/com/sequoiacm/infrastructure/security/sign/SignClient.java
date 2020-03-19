package com.sequoiacm.infrastructure.security.sign;

import java.util.Collection;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserJsonDeserializer;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.auth.ScmSessionService;
import com.sequoiacm.infrastructure.security.auth.ScmUserWrapper;

import feign.Response;

@Component
public class SignClient {
    private static final Logger logger = LoggerFactory.getLogger(SignClient.class);
    private SignService signService;
    private ScmSessionService sessionService;

    @Autowired
    public SignClient(ScmFeignClient feignClient) {
        signService = feignClient.builder().serviceTarget(SignService.class, "auth-server");
        sessionService = feignClient.builder().serviceTarget(ScmSessionService.class,
                "auth-server");
    }

    public void logout(String sessionId) throws ScmFeignException {
        sessionService.logout(sessionId);
    }

    public ScmUserWrapper getUserDetail(String sessionId) throws ScmFeignException {
        String sessionInfoStr = sessionService.getSession(sessionId, sessionId, true);
        BSONObject sessionInfo = (BSONObject) JSON.parse(sessionInfoStr);
        BSONObject userDetailsObj = (BSONObject) sessionInfo.get(RestField.USER_DETAILS);
        if (userDetailsObj == null) {
            logger.error("No user details in session: {}", sessionInfo);
            throw new RuntimeException("No user details in session");
        }
        return new ScmUserWrapper(ScmUserJsonDeserializer.deserialize(userDetailsObj),
                userDetailsObj.toString());

    }

    public String loginWithUsername(String username, String password) throws ScmFeignException {
        Response resp = signService.login(username, password);
        return parseLoginResp(resp);
    }

    public String loginWithSignature(SignatureInfo signature) throws ScmFeignException {
        Response resp = signService.login(signature.toBSON());
        return parseLoginResp(resp);
    }

    private String parseLoginResp(Response resp) throws ScmFeignException {
        if (resp.status() >= 200 && resp.status() < 300) {
            Collection<String> sessionIdCl = resp.headers().get(RestField.SESSION_ATTRIBUTE);
            if (sessionIdCl == null || sessionIdCl.size() <= 0) {
                throw new RuntimeException("failed to decode login resp, missing header:"
                        + RestField.SESSION_ATTRIBUTE);
            }

            return sessionIdCl.iterator().next();
        }

        Exception ex = new ScmFeignErrorDecoder().decode("login", resp);
        if (ex instanceof ScmFeignException) {
            throw (ScmFeignException) ex;
        }
        else {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String getSecretKey(String accesskey) throws ScmFeignException {
        BSONObject bson = signService.getSecretkey(accesskey);
        return new AccesskeyInfo(bson).getSecretkey();
    }

    public ScmUser getUser(String name) throws ScmFeignException {
        BSONObject user = signService.findUser(name);
        return ScmUserJsonDeserializer.deserialize(user);
    }
}
