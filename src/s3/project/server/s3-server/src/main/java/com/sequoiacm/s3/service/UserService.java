package com.sequoiacm.s3.service;

import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.s3.exception.S3ServerException;

public interface UserService {
    public AccesskeyInfo refreshAccesskey(String targetUser, String username,
            String encryptPassword) throws S3ServerException;
}
