package com.sequoiacm.contentserver.service;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;

public interface IPrivilegeService {
    void grant(String token, ScmUser user, String roleName, String resourceType, String resource,
            String privilege) throws ScmServerException;

    void revoke(String token, ScmUser user, String roleName, String resourceType, String resource,
            String privilege) throws ScmServerException;
}
