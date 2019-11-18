package com.sequoiacm.om.omserver.core;

import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;

@Component
public class ScmOmPasswordMgrImpl implements ScmOmPasswordMgr {
    private ScmPasswordMgr innerPasswordMgr;

    public ScmOmPasswordMgrImpl() {
        innerPasswordMgr = ScmPasswordMgr.getInstance();
    }

    @Override
    public String decrypt(String encryptedPassword) throws ScmOmServerException {
        if (encryptedPassword == null) {
            return null;
        }
        try {
            return innerPasswordMgr.decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, encryptedPassword);
        }
        catch (Exception e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to decrypt password:" + encryptedPassword, e);
        }
    }

    @Override
    public String encrypt(String srcPassword) throws ScmOmServerException {
        if (srcPassword == null) {
            return null;
        }

        try {
            return innerPasswordMgr.encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, srcPassword);
        }
        catch (Exception e) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "failed to encrypt password", e);
        }
    }

}
