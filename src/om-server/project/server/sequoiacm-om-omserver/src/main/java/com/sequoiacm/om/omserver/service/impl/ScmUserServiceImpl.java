package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import com.sequoiacm.om.omserver.factory.ScmUserDaoFactory;
import com.sequoiacm.om.omserver.module.OmUserFilter;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.service.ScmUserService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmUserServiceImpl implements ScmUserService {

    @Autowired
    private ScmUserDaoFactory scmUserDaoFactory;

    @Override
    public long getUserCount(ScmOmSession session, OmUserFilter userFilter)
            throws ScmInternalException, ScmOmServerException {
        return scmUserDaoFactory.createUserDao(session).countUser(userFilter);
    }

    @Override
    public OmUserInfo getUserInfo(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        return scmUserDaoFactory.createUserDao(session).getUser(username);
    }

    @Override
    public void createUser(ScmOmSession session, String username, String userType, String password)
            throws ScmInternalException {
        scmUserDaoFactory.createUserDao(session).createUser(username, userType, password);

    }

    @Override
    public void deleteUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).deleteUser(username);

    }

    @Override
    public void changePassword(ScmOmSession session, String username, String oldPassword,
            String newPassword, boolean cleanSessions)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).changePassword(username, oldPassword, newPassword,
                cleanSessions);
    }

    @Override
    public void grantRoles(ScmOmSession session, String username, List<String> roles)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).grantRoles(username, roles);
    }

    @Override
    public void revokeRoles(ScmOmSession session, String username, List<String> roles)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).revokeRoles(username, roles);
    }

    @Override
    public void changeUserType(ScmOmSession session, String username, String userType,
            String oldPassword, String newPassword)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).changeUserType(username, userType, oldPassword,
                newPassword);

    }

    @Override
    public void disableUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).disableUser(username);

    }

    @Override
    public void enableUser(ScmOmSession session, String username)
            throws ScmInternalException, ScmOmServerException {
        scmUserDaoFactory.createUserDao(session).enableUser(username);

    }

    @Override
    public List<OmUserInfo> listUsers(ScmOmSession session, OmUserFilter userFilter,
            long skip, int limit) throws ScmInternalException, ScmOmServerException {
        return scmUserDaoFactory.createUserDao(session).listUsers(userFilter, skip, limit);
    }

}
