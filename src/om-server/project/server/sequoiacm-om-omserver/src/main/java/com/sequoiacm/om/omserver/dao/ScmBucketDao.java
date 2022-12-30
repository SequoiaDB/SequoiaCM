package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface ScmBucketDao {

    Set<String> getUserAccessibleBuckets(String username) throws ScmInternalException;

    List<OmBucketDetail> listBucket(BSONObject condition, BSONObject orderBy, long skip, long limit)
            throws ScmInternalException;

    long countFile(String bucketName, BSONObject condition) throws ScmInternalException;

    List<OmFileBasic> listFile(String bucketName, BSONObject filter, BSONObject orderBy, long skip,
            int limit) throws ScmInternalException;

    OmBucketDetail getBucketDetail(String bucketName) throws ScmInternalException;

    void createFile(String bucketName, OmFileInfo fileInfo, BSONObject uploadConf, InputStream is)
            throws ScmInternalException;

    ScmBucket createBucket(ScmWorkspace workspace, String bucketName) throws ScmInternalException;

    void deleteBucket(String bucketName) throws ScmInternalException;

    void enableVersionControl(String bucketName) throws ScmInternalException;

    void suspendVersionControl(String bucketName) throws ScmInternalException;

    long countBucket(BSONObject filter) throws ScmInternalException;
}
