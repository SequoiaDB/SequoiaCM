package com.sequoiacm.infrastructrue.security.core;

public class ScmPrivMeta {
    public static final String JSON_FIELD_VERSION = "version";
    private int version;

    ScmPrivMeta() {
    }

    public ScmPrivMeta(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object rhs) {
        if (null == rhs) {
            return false;
        }

        if (rhs instanceof ScmPrivMeta) {
            return version == ((ScmPrivMeta) rhs).version;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScmPrivMeta{");
        sb.append(JSON_FIELD_VERSION).append(": ").append(version).append(" }");

        return sb.toString();
    }
}
