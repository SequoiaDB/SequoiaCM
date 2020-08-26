package com.sequoiacm.infrastructure.fulltext.core;

/**
 * Fulltext status for scm file.
 */
public enum ScmFileFulltextStatus {
    /**
     * Fulltext index is created.
     */
    CREATED,
    /**
     * Fulltext index is not created.
     */
    NONE,

    /**
     * Create fulltext index occur error.
     */
    ERROR;
}
