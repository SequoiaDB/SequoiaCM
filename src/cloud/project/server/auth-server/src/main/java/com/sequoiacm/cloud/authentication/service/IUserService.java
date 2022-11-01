package com.sequoiacm.cloud.authentication.service;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import org.bson.BSONObject;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface IUserService {

    ScmUser alterUser(Authentication authentication, String username, String oldPassword,
            String newPassword, ScmUserPasswordType passwordType, List<String> addRoles,
            List<String> delRoles, Boolean enabled, Boolean cleanSessions, boolean needToDecrypt)
            throws Exception;

    ScmUser createUser(String username, String type, String password, Authentication auth,
            boolean needToDecrypt) throws Exception;

    BSONObject findUserSalt(String username) throws Exception;
}
