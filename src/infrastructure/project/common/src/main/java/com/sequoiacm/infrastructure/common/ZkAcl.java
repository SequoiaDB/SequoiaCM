package com.sequoiacm.infrastructure.common;

public class ZkAcl {

    private boolean enabled = false;

    private String id;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void validate() {
        if (enabled) {
            checkNotEmpty(id, "id");
        }
    }

    private void checkNotEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
    }

    public boolean isIdAvailable(String idStr) {
        if (idStr == null || idStr.isEmpty()) {
            return false;
        }
        String[] split = idStr.split(":");
        return split.length == 2;
    }

}
