package com.sequoiacm.infrastructure.fulltext.core;
/**
 * Fulltext index mode.
 */
public enum ScmFulltextMode {
    /**
     * sync create index when file upload or update.
     */
    sync,
    
    /**
     * async create index when file upload or update.
     */
    async
}
