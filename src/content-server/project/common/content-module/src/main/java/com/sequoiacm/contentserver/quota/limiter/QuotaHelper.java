package com.sequoiacm.contentserver.quota.limiter;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuotaHelper {

    private static final Logger logger = LoggerFactory.getLogger(QuotaHelper.class);

    public static void checkQuota(long maxObjects, long maxSize, long acquireObjects,
            long acquireSize, long usedObjects, long usedSize, String bucketName)
            throws ScmServerException {
        if (maxObjects >= 0) { // 小于0表示不限制
            long remainObjects = maxObjects - usedObjects;
            if (acquireObjects > remainObjects) {
                logger.warn(
                        "bucket object count exceeded,bucketName={},acquireObjects={},remainObjects={}",
                        bucketName, acquireObjects, remainObjects);
                throw new ScmServerException(ScmError.BUCKET_QUOTA_EXCEEDED,
                        "bucket quota exceeded: bucketName=" + bucketName + ", maxObjects="
                                + maxObjects);
            }
        }
        if (maxSize >= 0) { // 小于0表示不限制
            long remainSize = maxSize - usedSize;
            if (acquireSize > remainSize) {
                logger.warn(
                        "bucket object size exceeded, bucketName={},acquireSize={},remainSize={}",
                        bucketName, acquireSize, remainSize);
                throw new ScmServerException(ScmError.BUCKET_QUOTA_EXCEEDED,
                        "bucket quota exceeded: bucketName: " + bucketName + ", maxSize: "
                                + ScmQuotaUtils.formatSize(maxSize));
            }
        }
    }
}
