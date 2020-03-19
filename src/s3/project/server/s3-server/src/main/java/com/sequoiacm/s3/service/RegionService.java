package com.sequoiacm.s3.service;

import com.sequoiacm.s3.exception.S3ServerException;

public interface RegionService {
    public void initWorkspaceS3Meta(String username, String encryptPassword, String ws)
            throws S3ServerException;
}
