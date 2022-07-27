package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
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
}
