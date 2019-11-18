package com.sequoiacm.om.omserver.core;

import com.sequoiacm.om.omserver.exception.ScmOmServerException;

public interface ScmOmPasswordMgr {
    public String decrypt(String encryptedPassword) throws ScmOmServerException;

    public String encrypt(String srcPassword) throws ScmOmServerException;
}
