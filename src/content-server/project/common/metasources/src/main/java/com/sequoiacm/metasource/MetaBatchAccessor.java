package com.sequoiacm.metasource;

import org.bson.BSONObject;

import com.sequoiacm.common.ScmShardingType;

public interface MetaBatchAccessor extends MetaAccessor {

    void attachFile(String batchId, String batchCreateMonth, String fileId, String user)
            throws ScmMetasourceException;

    void detachFile(String batchId, String batchCreateMonth, String fileId, String user)
            throws ScmMetasourceException;

    void delete(String batchId, String batchCreateMonth) throws ScmMetasourceException;

    boolean update(String batchId, String batchCreateMonth, BSONObject newBatchInfo)
            throws ScmMetasourceException;

    void createSubTable(ScmShardingType shardingType, String createMonth)
            throws ScmMetasourceException;

}
