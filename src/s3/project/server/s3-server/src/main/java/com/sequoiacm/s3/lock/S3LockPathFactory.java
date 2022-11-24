package com.sequoiacm.s3.lock;

import com.sequoiacm.infrastructure.lock.ScmLockPath;
import org.springframework.stereotype.Component;

@Component
public class S3LockPathFactory {
    private static final String UPLOADID = "s3_uploadid";
    private static final String PARTNUM = "partnum";

    public ScmLockPath createUploadLockPath(long uploadId) {
        String[] lockPath = { UPLOADID, Long.toString(uploadId) };
        return new ScmLockPath(lockPath);
    }

    public ScmLockPath createPartLockPath(long uploadId, int partNumber) {
        String[] lockPath = { UPLOADID, Long.toString(uploadId), PARTNUM,
                Integer.toString(partNumber) };
        return new ScmLockPath(lockPath);
    }
}
