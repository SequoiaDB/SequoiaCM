package com.sequoiacm.client.core;

import org.bson.BSONObject;

import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;

class ScmPrivilegeImpl implements ScmPrivilege {
    private static final String ROLE_TYPE_ROLE = "role";

    private ScmSession ss;

    private String id;
    private String roleType;
    private String roleId;
    private String resourceId;
    private String privilege;

    ScmPrivilegeImpl(ScmSession ss, String id, String roleType, String roleId, String resourceId,
            String privilege) {
        this.ss = ss;
        this.id = id;
        this.roleType = roleType;
        this.roleId = roleId;
        this.resourceId = resourceId;
        this.privilege = privilege;
    }

    ScmPrivilegeImpl(ScmSession ss, BSONObject obj) throws ScmException {
        this.ss = ss;
        fromBSONObj(obj);
    }

    private void fromBSONObj(BSONObject obj) throws ScmException {
        id = BsonUtils.getStringChecked(obj, FieldName.Privilege.FIELD_PRIVILEGE_ID);
        roleType = BsonUtils.getStringChecked(obj, FieldName.Privilege.FIELD_PRIVILEGE_ROLE_TYPE);
        roleId = BsonUtils.getStringChecked(obj, FieldName.Privilege.FIELD_PRIVILEGE_ROLE_ID);
        resourceId = BsonUtils.getStringChecked(obj,
                FieldName.Privilege.FIELD_PRIVILEGE_RESOURCE_ID);
        privilege = BsonUtils.getStringChecked(obj, FieldName.Privilege.FIELD_PRIVILEGE_PRIVILEGE);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRoleType() {
        return roleType;
    }

    @Override
    public String getRoleId() {
        return roleId;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public ScmRole getRole() throws ScmException {
        if (ROLE_TYPE_ROLE.equals(roleType)) {
            return ScmFactory.Role.getRoleById(ss, roleId);
        }

        return null;
    }

    @Override
    public ScmResource getResource() throws ScmException {
        return ScmFactory.Resource.getResourceById(ss, resourceId);
    }

    @Override
    public String getPrivilege() {
        return privilege;
    }

    @Override
    public ScmPrivilegeType getPrivilegeType() {
        return ScmPrivilegeType.getType(privilege);
    }

}
