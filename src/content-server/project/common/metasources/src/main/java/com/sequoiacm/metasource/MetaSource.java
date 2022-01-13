package com.sequoiacm.metasource;

public interface MetaSource {
    MetaAccessor createMetaAccessor(String tableName) throws ScmMetasourceException;

    MetaAccessor createMetaAccessor(String tableName, TransactionContext transactionContext)
            throws ScmMetasourceException;

    TransactionContext createTransactionContext() throws ScmMetasourceException;
}
