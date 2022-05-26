package com.sequoiacm.s3.scan;

import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.model.Upload;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.s3.exception.S3ServerException;

public class ListMultipartScanOffset extends S3ScanOffset {
    private String objKeyStartAfter;
    private Long uploadIdStartAfter;
    private boolean hasReach = false;
    private String commonPrefix;

    public ListMultipartScanOffset(String objKeyStartAfter, Long uploadIdStartAfter,
            String commonPrefix) throws S3ServerException {
        if (objKeyStartAfter != null && objKeyStartAfter.length() <= 0) {
            objKeyStartAfter = null;
        }
        this.commonPrefix = commonPrefix;

        if (commonPrefix != null) {
            if (objKeyStartAfter == null) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "objKeyStartAfter must be not null when commonPrefix is not null: commonPrefix="
                                + commonPrefix);
            }
            if (!objKeyStartAfter.startsWith(commonPrefix)) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "objKeyStartAfter must start with commonPrefix: objKeyStartAfter="
                                + objKeyStartAfter + ", commonPrefix=" + commonPrefix);
            }
        }

        this.objKeyStartAfter = objKeyStartAfter;
        this.uploadIdStartAfter = uploadIdStartAfter;
        this.commonPrefix = commonPrefix;
    }

    @Override
    public boolean isReach(BSONObject record) {
        if (hasReach) {
            return true;
        }

        if (objKeyStartAfter == null) {
            hasReach = true;
            return true;
        }

        String keyName = (String) record.get(UploadMeta.META_KEY_NAME);
        if (commonPrefix != null) {
            if (isReachCommonPrefix(keyName, commonPrefix)) {
                hasReach = true;
                return true;
            }
            return false;
        }

        int keyCompareResult = objKeyStartAfter.compareTo(keyName);
        if (keyCompareResult > 0) {
            return false;
        }
        if (keyCompareResult == 0) {
            if (hasSpecifyUploadIdStartAfter()) {
                long uploadId = (long) record.get(UploadMeta.META_UPLOAD_ID);
                int compare = uploadIdStartAfter.compareTo(uploadId);
                if (compare >= 0) {
                    return false;
                }
                hasReach = true;
                return true;
            }
            return false;
        }
        hasReach = true;
        return true;
    }

    @Override
    public BSONObject getOrderBy() {
        BasicBSONObject orderBy = new BasicBSONObject();
        orderBy.put(UploadMeta.META_KEY_NAME, 1);
        orderBy.put(UploadMeta.META_UPLOAD_ID, 1);
        return orderBy;
    }

    @Override
    public BSONObject getOptimizedMatcher() {
        BasicBSONObject majorKeyMatcher = new BasicBSONObject();
        if (objKeyStartAfter != null) {
            if (hasSpecifyUploadIdStartAfter()) {
                majorKeyMatcher.put("$gte", objKeyStartAfter);
            }
            else {
                majorKeyMatcher.put("$gt", objKeyStartAfter);
            }
            return new BasicBSONObject(UploadMeta.META_KEY_NAME, majorKeyMatcher);
        }
        return null;

    }

    boolean hasSpecifyUploadIdStartAfter() {
        return uploadIdStartAfter != null;
    }

    @Override
    public S3ScanOffset createOffsetByRecord(BSONObject b, String commonPrefix)
            throws S3ServerException {
        return new ListMultipartScanOffset((String) b.get(UploadMeta.META_KEY_NAME),
                (Long) b.get(UploadMeta.META_UPLOAD_ID), commonPrefix);
    }

    @Override
    public String toString() {
        return "ListMultipartScanOffset{" + "objKeyStartAfter='" + objKeyStartAfter + '\''
                + ", uploadIdStartAfter=" + uploadIdStartAfter + ", hasReach=" + hasReach
                + ", commonPrefix='" + commonPrefix + '\'' + ", orderBy=" + getOrderBy()
                + ", optimizedMatcher=" + getOptimizedMatcher() + '}';
    }

    public String getObjKeyStartAfter() {
        return objKeyStartAfter;
    }

    public Long getUploadIdStartAfter() {
        return uploadIdStartAfter;
    }

    public String getCommonPrefix() {
        return commonPrefix;
    }
}
