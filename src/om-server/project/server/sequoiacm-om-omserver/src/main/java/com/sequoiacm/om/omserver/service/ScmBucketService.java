package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBatchOpResult;
import com.sequoiacm.om.omserver.module.OmBucketCreateInfo;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
import com.sequoiacm.om.omserver.module.OmBucketUpdateInfo;
import com.sequoiacm.om.omserver.module.OmDeltaStatistics;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.List;

public interface ScmBucketService {

    List<String> getUserAccessibleBuckets(ScmOmSession session)
            throws ScmInternalException, ScmOmServerException;

    OmBucketDetail getBucketDetail(ScmOmSession session, String bucketName)
            throws ScmOmServerException, ScmInternalException;

    long countFile(ScmOmSession session, String bucketName, BSONObject condition)
            throws ScmOmServerException, ScmInternalException;

    List<OmFileBasic> listFiles(ScmOmSession session, String bucketName, BSONObject filter,
            BSONObject orderBy, long skip, int limit)
            throws ScmOmServerException, ScmInternalException;

    void createFile(ScmOmSession session, String bucketName, String siteName, OmFileInfo fileInfo,
            BSONObject uploadConf, InputStream is) throws ScmInternalException;

    List<OmBucketDetail> listBucket(ScmOmSession session, BSONObject filter, BSONObject orderBy,
            long skip, int limit, Boolean isStrictMode)
            throws ScmInternalException, ScmOmServerException;

    List<OmBatchOpResult> createBucket(ScmOmSession session, OmBucketCreateInfo bucketCreateInfo)
            throws ScmOmServerException, ScmInternalException;

    List<OmBatchOpResult> deleteBuckets(ScmOmSession session, List<String> bucketNames)
            throws ScmOmServerException, ScmInternalException;

    void updateBucket(ScmOmSession session, String bucketName, OmBucketUpdateInfo bucketUpdateInfo)
            throws ScmOmServerException, ScmInternalException;

    long countBucket(ScmOmSession session, BSONObject filter, Boolean isStrictMode)
            throws ScmOmServerException, ScmInternalException;

    OmDeltaStatistics getObjectDelta(ScmOmSession session, String bucketName, Long beginTime,
            Long endTime) throws ScmOmServerException, ScmInternalException;

    List<OmBucketDetail> listBucketFilterQuotaLevel(ScmOmSession session, BSONObject filter,
            BSONObject orderBy, Boolean isStrictMode, String quotaLevel)
            throws ScmOmServerException, ScmInternalException;
}
