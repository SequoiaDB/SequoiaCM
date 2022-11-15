
package com.sequoiacm.om.omserver.service;

import java.util.List;

import com.sequoiacm.om.omserver.module.OmUserFilter;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmUserService {

    public long getUserCount(ScmOmSession session, OmUserFilter userFilter)
            throws ScmInternalException, ScmOmServerException;

    public OmUserInfo getUserInfo(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException;

    public void createUser(ScmOmSession session, String username, String userType, String password)
            throws ScmInternalException, ScmOmServerException;

    public void deleteUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException;

    public void changePassword(ScmOmSession session, String username, String oldPassword,
            String newPassword, boolean cleanSessions)
            throws ScmInternalException, ScmOmServerException;

    public void grantRoles(ScmOmSession session, String username, List<String> roles)
            throws ScmInternalException, ScmOmServerException;

    public void revokeRoles(ScmOmSession session, String username, List<String> roles)
            throws ScmInternalException, ScmOmServerException;

    public void changeUserType(ScmOmSession session, String username, String userType,
            String oldPassword, String newPassword)
            throws ScmInternalException, ScmOmServerException;

    public void disableUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException;

    public void enableUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException;

    public List<OmUserInfo> listUsers(ScmOmSession session, OmUserFilter userFilter,
            long skip, int limit) throws ScmInternalException, ScmOmServerException;
}
