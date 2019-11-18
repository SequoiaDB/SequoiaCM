package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

class ScmRoleImpl implements ScmRole {
    private String roleId;
    private String roleName;
    private String description;

    ScmRoleImpl(String roleId, String roleName, String description) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
    }

    ScmRoleImpl(BSONObject obj) throws ScmException {
        if (obj == null) {
            throw new ScmInvalidArgumentException("obj is null");
        }
        fromBSONObj(obj);
    }

    private void fromBSONObj(BSONObject obj) throws ScmException {
        roleId = BsonUtils.getStringChecked(obj, FieldName.Role.FIELD_ROLE_ID);
        roleName = BsonUtils.getStringChecked(obj, FieldName.Role.FIELD_ROLE_NAME);
        description = BsonUtils.getString(obj, FieldName.Role.FIELD_DESCRIPTION);
        if (Strings.isEmpty(description)) {
            description = "";
        }
    }

    @Override
    public String getRoleId() {
        return roleId;
    }

    @Override
    public String getRoleName() {
        return roleName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScmRole) {
            return roleId.equals(((ScmRole) obj).getRoleId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return roleId.hashCode();
    }
}
