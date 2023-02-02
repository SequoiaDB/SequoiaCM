package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.om.omserver.module.OmUserFilter;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmUserDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmUserDaoImpl implements ScmUserDao {

    private ScmOmSession session;
    private static final Logger logger = LoggerFactory.getLogger(ScmUserDaoImpl.class);

    public ScmUserDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public OmUserInfo getUser(String username) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            return transformUser(user);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to get user, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void createUser(String username, String userType, String password)
            throws ScmInternalException {
        try {
            ScmUserPasswordType scmUserType = ScmUserPasswordType.valueOf(userType);
            ScmFactory.User.createUser(session.getConnection(), username, scmUserType, password);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to create user, " + e.getMessage(),
                    e);
        }

    }

    @Override
    public void deleteUser(String username) throws ScmInternalException {
        try {
            ScmFactory.User.deleteUser(session.getConnection(), username);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to delete user, " + e.getMessage(),
                    e);
        }

    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword,
            boolean cleanSessions) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.setPassword(oldPassword, newPassword);
            modifier.setCleanSessions(cleanSessions);
            ScmFactory.User.alterUser(session.getConnection(), user, modifier);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to change passowrd, " + e.getMessage(), e);
        }

    }

    @Override
    public void grantRoles(String username, List<String> roles) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            ScmUserModifier modifier = new ScmUserModifier().addRoleNames(roles);
            ScmFactory.User.alterUser(session.getConnection(), user, modifier);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to grant roles, " + e.getMessage(),
                    e);
        }

    }

    @Override
    public void revokeRoles(String username, List<String> roles) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            ScmUserModifier modifier = new ScmUserModifier().delRoleNames(roles);
            ScmFactory.User.alterUser(session.getConnection(), user, modifier);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to revoke roles, " + e.getMessage(), e);
        }

    }

    @Override
    public void changeUserType(String username, String userType, String oldPassword,
            String newPassword) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            ScmUserPasswordType scmUserType = ScmUserPasswordType.valueOf(userType);
            ScmUserModifier modifier = new ScmUserModifier().setPasswordType(scmUserType);
            if (scmUserType == ScmUserPasswordType.LOCAL) {
                modifier.setPassword(oldPassword, newPassword);
            }
            ScmFactory.User.alterUser(session.getConnection(), user, modifier);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to change user type, " + e.getMessage(), e);
        }
    }

    @Override
    public void disableUser(String username) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            ScmUserModifier modifier = new ScmUserModifier().setEnabled(false);
            ScmFactory.User.alterUser(session.getConnection(), user, modifier);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to disable user, " + e.getMessage(), e);
        }
    }

    @Override
    public void enableUser(String username) throws ScmInternalException {
        try {
            ScmUser user = ScmFactory.User.getUser(session.getConnection(), username);
            ScmUserModifier modifier = new ScmUserModifier().setEnabled(true);
            ScmFactory.User.alterUser(session.getConnection(), user, modifier);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to enable user, " + e.getMessage(),
                    e);
        }

    }

    @Override
    public List<OmUserInfo> listUsers(OmUserFilter userFilter, long skip, int limit)
            throws ScmInternalException {
        ScmCursor<ScmUser> cursor = null;
        List<OmUserInfo> res = new ArrayList<>();
        try {
            cursor = ScmFactory.User.listUsers(session.getConnection(),
                    generateCondition(userFilter), 0, -1);
            long counter = 0;
            while (cursor.hasNext()) {
                ScmUser user = cursor.getNext();
                if (++counter <= skip) {
                    continue;
                }
                if (userFilter.getNameMatcher() == null
                        || user.getUsername().contains(userFilter.getNameMatcher())) {
                    res.add(transformUser(user));
                }
                if (limit != -1 && limit == res.size()) {
                    break;
                }
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to list users, " + e.getMessage(),
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private BSONObject generateCondition(OmUserFilter userFilter) {
        String FIELD_HAS_ROLE = "has_role";
        String FIELD_ENABLED = "enabled";

        BSONObject condition = new BasicBSONObject();
        condition.put(FIELD_HAS_ROLE, userFilter.getHasRole());
        condition.put(FIELD_ENABLED, userFilter.getEnabled());
        return condition;
    }

    @Override
    public long countUser(OmUserFilter userFilter) throws ScmInternalException {
        return listUsers(userFilter, 0, -1).size();
    }

    private OmUserInfo transformUser(ScmUser user) {
        OmUserInfo userInfo = new OmUserInfo();
        userInfo.setUserName(user.getUsername());
        userInfo.setEnable(user.isEnabled());
        userInfo.setUserId(user.getUserId());
        userInfo.setUserType(user.getPasswordType().toString());
        List<OmRoleBasicInfo> rolesInfo = new ArrayList<>();
        for (ScmRole role : user.getRoles()) {
            rolesInfo.add(transformRole(role));
        }
        userInfo.setRoles(rolesInfo);
        return userInfo;
    }

    private OmRoleBasicInfo transformRole(ScmRole role) {
        return new OmRoleBasicInfo(role.getRoleId(), role.getRoleName(), role.getDescription());
    }

}
