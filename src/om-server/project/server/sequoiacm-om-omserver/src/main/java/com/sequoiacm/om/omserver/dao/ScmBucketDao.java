package com.sequoiacm.om.omserver.dao;

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

    long countFile(String bucketName, BSONObject condition) throws ScmInternalException;

    List<OmFileBasic> listFile(String bucketName, BSONObject filter, BSONObject orderBy, long skip,
            int limit) throws ScmInternalException;

    OmBucketDetail getBucketDetail(String bucketName) throws ScmInternalException;

    void createFile(String bucketName, OmFileInfo fileInfo, BSONObject uploadConf, InputStream is)
            throws ScmInternalException;
}
