package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaSource {
    MetaAccessor createMetaAccessor(String tableName) throws ScmMetasourceException;

    MetaAccessor createMetaAccessor(String tableName, TransactionContext transactionContext)
            throws ScmMetasourceException;

    TransactionContext createTransactionContext() throws ScmMetasourceException;

    BSONObject getMetaSourceTask(long taskId) throws ScmMetasourceException;

    void cancelMetaSourceTask(long taskId, boolean isAsync) throws ScmMetasourceException;

    MetasourceVersion getVersion() throws ScmMetasourceException;
}
