package com.sequoiacm.cloud.adminserver.metasource;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;

public interface MetaAccessor {
    public void insert(BSONObject insertor) throws ScmMetasourceException;

    public void upsert(BSONObject upsertor, BSONObject matcher) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException;

    public void ensureIndex(String indexName, BSONObject indexDefinition, boolean isUnique)
            throws ScmMetasourceException;

    public BSONObject queryOne(BSONObject matcher) throws ScmMetasourceException;

    public void ensureTable() throws ScmMetasourceException;

    // public void delete(BSONObject deletor) throws ScmInnerException;

    // public void update(BSONObject matcher, BSONObject updator) throws
    // ScmInnerException;
    // public boolean updateAndCheck(BSONObject matcher, BSONObject updator) throws
    // ScmInnerException;
}
