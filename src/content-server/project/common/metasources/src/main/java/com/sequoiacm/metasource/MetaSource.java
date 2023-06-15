package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaSource {
    MetaAccessor createMetaAccessor(String tableName) throws ScmMetasourceException;

    MetaAccessor createMetaAccessor(String tableName, TransactionContext transactionContext)
            throws ScmMetasourceException;

    TransactionContext createTransactionContext() throws ScmMetasourceException;

    BSONObject getMetaSourceTask(long taskId) throws ScmMetasourceException;

    void cancelMetaSourceTask(long taskId, boolean isAsync) throws ScmMetasourceException;

    // 回退sdb驱动至349，不支持 getVersion：SEQUOIACM-1411
    //MetasourceVersion getVersion() throws ScmMetasourceException;

    /**
     * 设置当前线程的连接选项， 只影响当前线程下获取的连接属性
     */
    void setConnOptionsLocal(ConnOptions options);

    /**
     * 清除当前线程的 sdb 连接选项
     */
    void removeConnOptionsLocal();
}
