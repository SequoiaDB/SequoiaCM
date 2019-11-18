package com.sequoiacm.infrastructrue.security.core;

public class ScmPrivilege {
    public static final String JSON_FIELD_ID = "id";
    public static final String JSON_FIELD_ROLE_TYPE = "role_type";
    public static final String JSON_FIELD_ROLE_ID = "role_id";
    public static final String JSON_FIELD_RESOURCE_ID = "resource_id";
    public static final String JSON_FIELD_PRIVILEGE = "privilege";

    public static final String JSON_VALUE_ROLE_TYPE_ROLE = "role";

    private String id;
    private String roleType;
    private String roleId;
    private String resourceId;
    private String privilege;

    ScmPrivilege() {
    }

    public ScmPrivilege(String id, String roleType, String roleId, String resourceId,
            String privilege) {
        this.id = id;
        this.roleType = roleType;
        this.roleId = roleId;
        this.resourceId = resourceId;
        this.privilege = privilege;
    }

    public String getId() {
        return id;
    }

    public String getRoleType() {
        return roleType;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getPrivilege() {
        return privilege;
    }

    @Override
    public boolean equals(Object rhs) {
        if (null == rhs) {
            return false;
        }

        if (rhs instanceof ScmPrivilege) {
            ScmPrivilege right = (ScmPrivilege) rhs;
            return isStrEquals(id, right.getId()) && isStrEquals(roleType, right.getRoleType())
                    && isStrEquals(roleId, right.getRoleId())
                    && isStrEquals(resourceId, right.getResourceId())
                    && isStrEquals(privilege, right.getPrivilege());
        }

        return false;
    }

    private boolean isStrEquals(String left, String right) {
        if (null != left) {
            return left.equals(right);
        }
        else {
            if (null != right) {
                return false;
            }
            else {
                return true;
            }
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScmPrivilege{ ");
        sb.append(JSON_FIELD_ID).append(": ").append(id).append(", ");
        sb.append(JSON_FIELD_ROLE_TYPE).append(": ").append(roleType).append(", ");
        sb.append(JSON_FIELD_ROLE_ID).append(": ").append(roleId).append(", ");
        sb.append(JSON_FIELD_RESOURCE_ID).append(": ").append(resourceId).append(", ");
        sb.append(JSON_FIELD_PRIVILEGE).append(": ").append(privilege);
        sb.append(" }");

        return sb.toString();
    }

}
