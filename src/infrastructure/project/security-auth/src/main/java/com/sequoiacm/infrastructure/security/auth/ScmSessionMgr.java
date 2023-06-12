package com.sequoiacm.infrastructure.security.auth;

import com.sequoiacm.infrastructure.common.SecurityRestField;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserJsonDeserializer;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;

public class ScmSessionMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmSessionMgr.class);
    private ScmSessionService scmSessionService;

    public ScmSessionMgr(ScmFeignClient feignClient) {
        scmSessionService = feignClient.builder().serviceTarget(ScmSessionService.class, "auth-server");
    }

    public ScmUserWrapper getUserBySessionId(String sessionId) throws ScmFeignException {
        String sessionInfoStr = scmSessionService.getSession(sessionId, sessionId, true);
        BSONObject sessionInfo = (BSONObject) JSON.parse(sessionInfoStr);
        BSONObject userDetailsObj = (BSONObject) sessionInfo.get(SecurityRestField.USER_DETAILS);
        if (userDetailsObj == null) {
            logger.error("No user details in session: {}", sessionInfo);
            throw new RuntimeException("No user details in session");
        }

        ScmUser user = ScmUserJsonDeserializer.deserialize(userDetailsObj);
        return new ScmUserWrapper(user, userDetailsObj.toString());
    }

    public void markSessionLogout(String sessionId) {
        // no cache do noting
    }

    public void close() {
        // do noting
    }
}
