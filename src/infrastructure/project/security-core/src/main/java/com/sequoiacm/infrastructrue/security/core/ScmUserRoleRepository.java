package com.sequoiacm.infrastructrue.security.core;

import java.util.List;

import org.bson.BSONObject;

public interface ScmUserRoleRepository {

    String generateUserId();

    void insertUser(ScmUser user);

    /**
     * Cannot update userId and username
     *
     * @param user
     *            user to modified
     * @param t
     *            transaction
     */
    void updateUser(ScmUser user, ITransaction t);

    void deleteUser(ScmUser user);

    void deleteUser(ScmUser user, ITransaction t);

    ScmUser findUserByName(String userName);

    ScmUser findUserById(String userId);

    List<ScmUser> findUsersByRoleName(String roleName);

    List<ScmUser> findAllUsers(ScmUserPasswordType type, Boolean enabled, String innerRoleName,
            BSONObject orderBy, long skip, long limit);

    String generateRoleId();

    void insertRole(ScmRole role);

    void deleteRoleByName(String roleName);

    void deleteRoleById(String roleId);

    void deleteRole(ScmRole role);

    ScmRole findRoleByName(String roleName);

    ScmRole findRoleById(String roleId);

    List<ScmRole> findAllRoles(BSONObject orderBy, long skip, long limit);

    void deleteRole(ScmRole role, ITransaction t);

    void deleteRoleById(String roleId, ITransaction t);

    void deleteRoleFromUser(ScmUser user, String roleId, ITransaction t);
}
