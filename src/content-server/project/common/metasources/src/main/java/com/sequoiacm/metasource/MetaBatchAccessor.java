package com.sequoiacm.metasource;

import org.bson.BSONObject;

public interface MetaBatchAccessor extends MetaAccessor {
    
    void attachFile(String batchId, String fileId, String user) 
            throws ScmMetasourceException;
    
    void detachFile(String batchId, String fileId, String user) 
            throws ScmMetasourceException;
    
    void delete(String batchId) throws ScmMetasourceException;

    boolean update(String batchId, BSONObject newBatchInfo) 
            throws ScmMetasourceException;

}
