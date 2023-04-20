package com.sequoiacm.config.framework.bucket.metasource;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.common.TableCreatedResult;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface BucketMetaService {
    TableCreatedResult createBucketFileTable(String wsName, long bucketId)
            throws ScmConfigException;

    TableDao getBucketTable(Transaction transaction);

    long genBucketId() throws MetasourceException;

    void dropBucketFileTableSilence(String tableName);
}
