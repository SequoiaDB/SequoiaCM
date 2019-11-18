package com.sequoiacm.infrastructrue.security.core;

public class ScmResource {
    public static final String JSON_FIELD_ID = "id";
    public static final String JSON_FIELD_TYPE = "type";
    public static final String JSON_FIELD_RESOURCE = "resource";

    private String id;
    private String type;
    private String resource;

    ScmResource() {
    }

    public ScmResource(String id, String type, String resource) {
        this.id = id;
        this.type = type;
        this.resource = resource;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public boolean equals(Object rhs) {
        if (null == rhs) {
            return false;
        }

        if (rhs instanceof ScmResource) {
            ScmResource right = (ScmResource) rhs;
            return isStrEquals(id, right.getId()) && isStrEquals(type, right.getType())
                    && isStrEquals(resource, right.getResource());
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
        sb.append("ScmResource{ ");
        sb.append(JSON_FIELD_ID).append(": ").append(id).append(",");
        sb.append(JSON_FIELD_TYPE).append(": ").append(type).append(",");
        sb.append(JSON_FIELD_RESOURCE).append(": ").append(resource);
        sb.append(" }");

        return sb.toString();
    }
}
