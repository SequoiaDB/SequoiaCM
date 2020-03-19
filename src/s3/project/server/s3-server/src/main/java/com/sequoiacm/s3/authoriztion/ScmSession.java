package com.sequoiacm.s3.authoriztion;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.security.auth.ScmUserWrapper;

public class ScmSession {
    private String secretkey;
    private String sessionId;
    private String userDetail;
    private ScmUser user;

    public ScmSession(String secretkey, String sessionId, ScmUserWrapper userWrapper) {
        this.secretkey = secretkey;
        this.sessionId = sessionId;
        this.userDetail = userWrapper.getUserJSON();
        this.user = userWrapper.getUser();
    }

    public String getSecretkey() {
        return secretkey;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ScmUser getUser() {
        return user;
    }

    public String getUserDetail() {
        return userDetail;
    }

}
