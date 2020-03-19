package com.sequoiacm.s3.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.authoriztion.ScmSessionMgr;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.AuthServerClient;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private ScmSessionMgr sessionMgr;

    @Autowired
    private ScmClientFactory csClientFactory;

    @Override
    public AccesskeyInfo refreshAccesskey(String targetUser, String username,
            String encryptPassword) throws S3ServerException {
        String srcPassword;
        try {
            srcPassword = ScmPasswordMgr.getInstance().decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES,
                    encryptPassword);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INVALID_ARGUMENT, "decrypt password failed", e);
        }
        ScmSession session = sessionMgr.createSessionByUsername(username, srcPassword);
        try {
            AuthServerClient client = csClientFactory.getAuthServerClient(session);
            if (targetUser.equals(username)) {
                return client.refreshAccesskey(targetUser, encryptPassword);
            }
            return client.refreshAccesskey(targetUser, null);
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.USER_UPDATE_FAILED,
                    "refresh accesskey failed:" + username, e);
        }
        finally {
            sessionMgr.logoutSession(session);
        }
    }

}
