package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.om.omserver.module.OmUserFilter;
import org.bson.BSONObject;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmUserInfo;

public interface ScmUserDao {
    public OmUserInfo getUser(String username) throws ScmInternalException;

    public void createUser(String username, String userType, String password)
            throws ScmInternalException;

    public void deleteUser(String username) throws ScmInternalException;

    public void changePassword(String username, String oldPassword, String newPassword,
            boolean cleanSessions) throws ScmInternalException;

    public void grantRoles(String username, List<String> roles) throws ScmInternalException;

    public void revokeRoles(String username, List<String> roles) throws ScmInternalException;

    public void changeUserType(String username, String userType, String oldPassword,
            String newPassword) throws ScmInternalException;

    public void disableUser(String username) throws ScmInternalException;

    public void enableUser(String username) throws ScmInternalException;

    public List<OmUserInfo> listUsers(OmUserFilter userFilter, long skip,
            int limit) throws ScmInternalException;

    long countUser(OmUserFilter userFilter) throws ScmInternalException;
}
