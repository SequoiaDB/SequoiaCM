package com.sequoiacm.common.module;

public enum ScmBucketVersionStatus {
    /**
     * Bucket version control is disabled.
     */
    Disabled,

    /**
     * Bucket version control is enabled.
     */
    Enabled,

    /**
     * Bucket version control is suspended.
     */
    Suspended;

    public static ScmBucketVersionStatus parse(String status) {
        for (ScmBucketVersionStatus s : ScmBucketVersionStatus.values()) {
            if (s.name().equals(status)) {
                return s;
            }
        }
        return null;
    }
}
