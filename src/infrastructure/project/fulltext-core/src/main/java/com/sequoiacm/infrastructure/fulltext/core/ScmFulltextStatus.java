package com.sequoiacm.infrastructure.fulltext.core;

/**
 * Fulltext status for workspace.
 */
public enum ScmFulltextStatus {
    /**
     * Fulltext index is creating.
     */
    CREATING,

    /**
     * Fulltext index is deleting.
     */
    DELETING,

    /**
     * Fulltext index is created.
     */
    CREATED,

    /**
     * Fulltext index is not created.
     */
    NONE;
}
