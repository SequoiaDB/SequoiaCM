package com.sequoiacm.s3.remote;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;

public class AuthServerClient {
    private ScmSession session;
    private AuthServerService service;

    public AuthServerClient(ScmSession session, AuthServerService service) {
        this.session = session;
        this.service = service;
    }

    public AccesskeyInfo refreshAccesskey(String user, String encryptPasswd)
            throws ScmFeignException {
        return service.refreshAccesskey(session.getSessionId(), session.getUserDetail(), "refresh",
                user, encryptPasswd);
    }
}
