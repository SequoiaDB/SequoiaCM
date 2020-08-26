package com.sequoiacm.fulltext.server.service;

import org.bson.BSONObject;

import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltexInfo;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;

public interface FulltextService {
    void createIndex(String ws, BSONObject fileMatcher, ScmFulltextMode mode)
            throws FullTextException;

    void dropIndex(String ws) throws FullTextException;

    ScmFulltexInfo getIndexInfo(String ws) throws FullTextException;

    ScmFileFulltextInfoCursor getFileIndexInfo(String ws, ScmFileFulltextStatus status)
            throws FullTextException;

    long countFileWithIdxStatus(String ws, ScmFileFulltextStatus status) throws FullTextException;

    void updateIndex(String ws, BSONObject newFileMatcher, ScmFulltextMode newMode)
            throws FullTextException;

    public FulltextSearchCursor search(String ws, int scope, BSONObject contentCondition,
            BSONObject fileCondition) throws FullTextException;

    void rebuildIndex(String ws, String fileId) throws FullTextException;

    public void inspect(String ws) throws FullTextException;
}
