package com.sequoiacm.client.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;

class ScmUserImpl implements ScmUser {
    private String userId;
    private String username;
    private ScmUserPasswordType passwordType;
    private Set<ScmRole> roles;
    private boolean enabled;

    ScmUserImpl(BSONObject obj) throws ScmException {
        if (obj == null) {
            throw new ScmSystemException("obj is null");
        }
        fromBSONObj(obj);
    }

    private void fromBSONObj(BSONObject obj) throws ScmException {
        userId = BsonUtils.getStringChecked(obj, FieldName.User.FIELD_USER_ID);
        username = BsonUtils.getStringChecked(obj, FieldName.User.FIELD_USERNAME);
        passwordType = ScmUserPasswordType
                .valueOf(BsonUtils.getStringChecked(obj, FieldName.User.FIELD_PASSWORD_TYPE));
        enabled = BsonUtils.getBooleanChecked(obj, FieldName.User.FIELD_ENABLED);
        BasicBSONList rolesList = BsonUtils.getArray(obj, FieldName.User.FIELD_ROLES);
        if (rolesList == null || rolesList.isEmpty()) {
            roles = Collections.emptySet();
        }
        else {
            Set<ScmRole> rolesSet = new TreeSet<ScmRole>(new ScmRoleComparator());
            for (Object o : rolesList) {
                BSONObject roleObj = (BSONObject) o;
                ScmRole role = new ScmRoleImpl(roleObj);
                rolesSet.add(role);
            }

            roles = Collections.unmodifiableSet(rolesSet);
        }
    }

    private static class ScmRoleComparator implements Comparator<ScmRole>, Serializable {

        @Override
        public int compare(ScmRole o1, ScmRole o2) {
            return o1.getRoleId().compareTo(o2.getRoleId());
        }
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public ScmUserPasswordType getPasswordType() {
        return passwordType;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<ScmRole> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(ScmRole role) {
        for (ScmRole r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(String roleName) {
        if (!Strings.hasText(roleName)) {
            return false;
        }
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }
        for (ScmRole r : roles) {
            if (r.getRoleName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScmUser) {
            return userId.equals(((ScmUser) obj).getUserId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
