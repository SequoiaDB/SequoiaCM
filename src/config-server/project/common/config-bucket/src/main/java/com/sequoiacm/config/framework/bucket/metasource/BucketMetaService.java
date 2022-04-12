package com.sequoiacm.config.framework.bucket.metasource;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface BucketMetaService {
    String createBucketFileTable(String wsName, long bucketId) throws ScmConfigException;

    TableDao getBucketTable(Transaction transaction);

    long genBucketId() throws MetasourceException;

    void dropBucketFileTableSilence(String tableName) throws MetasourceException;
}
