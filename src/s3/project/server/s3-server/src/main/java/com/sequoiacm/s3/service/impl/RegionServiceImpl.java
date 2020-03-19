package com.sequoiacm.s3.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.authoriztion.ScmSessionMgr;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiacm.s3.service.RegionService;

@Component
public class RegionServiceImpl implements RegionService {

    @Autowired
    private ScmClientFactory csClientFactory;

    @Autowired
    ScmSessionMgr sessionMgr;

    @Override
    public void initWorkspaceS3Meta(String username, String encryptPassword, String ws)
            throws S3ServerException {
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
            ScmContentServerClient client = csClientFactory.getContentServerClient(session, ws);
            client.initWorkspaceS3Meta();
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.REGION_INIT_FAILED,
                    "failed to init region, failed to create s3 meta", e);
        }
        finally {
            sessionMgr.logoutSession(session);
        }
    }

}
