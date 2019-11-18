package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.service.ScmUserService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmUserServiceImpl implements ScmUserService {

    @Override
    public OmUserInfo getUserInfo(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        return session.getUserDao().getUser(username);
    }

    @Override
    public void createUser(ScmOmSession session, String username, String userType, String password)
            throws ScmInternalException {
        session.getUserDao().createUser(username, userType, password);

    }

    @Override
    public void deleteUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().deleteUser(username);

    }

    @Override
    public void changePassword(ScmOmSession session, String username, String oldPassword,
            String newPassword, boolean cleanSessions)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().changePassword(username, oldPassword, newPassword, cleanSessions);
    }

    @Override
    public void grantRoles(ScmOmSession session, String username, List<String> roles)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().grantRoles(username, roles);
    }

    @Override
    public void revokeRoles(ScmOmSession session, String username, List<String> roles)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().revokeRoles(username, roles);
    }

    @Override
    public void changeUserType(ScmOmSession session, String username, String userType,
            String oldPassword, String newPassword)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().changeUserType(username, userType, oldPassword, newPassword);

    }

    @Override
    public void disableUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().disableUser(username);

    }

    @Override
    public void enableUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        session.getUserDao().enableUser(username);

    }

    @Override
    public List<OmUserInfo> listUsers(ScmOmSession session, BSONObject condition, long skip,
            int limit) throws ScmInternalException, ScmOmServerException {
        return session.getUserDao().listUsers(condition, skip, limit);
    }

}
