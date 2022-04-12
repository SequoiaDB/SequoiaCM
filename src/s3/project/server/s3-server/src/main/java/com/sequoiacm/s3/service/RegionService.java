package com.sequoiacm.s3.service;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.s3.exception.S3ServerException;

public interface RegionService {
    void setDefaultRegion(String ws) throws ScmServerException;

    String getDefaultRegionForScm() throws ScmServerException;

    String getDefaultRegionForS3() throws S3ServerException;
}
