package com.sequoiacm.s3.scan;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class ListObjectScanOffset extends S3ScanOffset {

    private boolean hasReach;
    private String objKeyStartAfter;
    private String commonPrefix;

    public ListObjectScanOffset(String objKeyStartAfter, String commonPrefix)
            throws S3ServerException {
        if (objKeyStartAfter != null && objKeyStartAfter.length() <= 0) {
            objKeyStartAfter = null;
        }
        this.commonPrefix = commonPrefix;

        if (commonPrefix != null) {
            if (objKeyStartAfter == null) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "majorKeyStartAfter must be not null when commonPrefix is not null: commonPrefix="
                                + commonPrefix);
            }
            if (!objKeyStartAfter.startsWith(commonPrefix)) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "majorKeyStartAfter must start with commonPrefix: objKeyStartAfter="
                                + objKeyStartAfter + ", commonPrefix=" + commonPrefix);
            }
        }

        this.objKeyStartAfter = objKeyStartAfter;
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

        String fileName = (String) record.get(FieldName.BucketFile.FILE_NAME);
        if (commonPrefix != null) {
            if (isReachCommonPrefix(fileName, commonPrefix)) {
                hasReach = true;
                return true;
            }
            return false;
        }

        int majorKeyCompareResult = objKeyStartAfter.compareTo(fileName);
        if (majorKeyCompareResult > 0) {
            return false;
        }
        if (majorKeyCompareResult == 0) {
            return false;
        }
        hasReach = true;
        return true;
    }

    @Override
    public BSONObject getOrderBy() {
        BasicBSONObject orderBy = new BasicBSONObject();
        orderBy.put(FieldName.BucketFile.FILE_NAME, 1);
        return orderBy;
    }

    @Override
    public BSONObject getOptimizedMatcher() {
        BasicBSONObject majorKeyMatcher = new BasicBSONObject();
        if (objKeyStartAfter != null) {
            majorKeyMatcher.put("$gt", objKeyStartAfter);
            return new BasicBSONObject(FieldName.BucketFile.FILE_NAME, majorKeyMatcher);
        }
        return null;

    }

    @Override
    public S3ScanOffset createOffsetByRecord(BSONObject b, String commonPrefix)
            throws S3ServerException {
        return new ListObjectScanOffset((String) b.get(FieldName.BucketFile.FILE_NAME),
                commonPrefix);
    }

    @Override
    public String toString() {
        return "ListObjectScanOffset{" + "hasReach=" + hasReach + ", objKeyStartAfter='"
                + objKeyStartAfter + '\'' + ", commonPrefix='" + commonPrefix + '\'' + ", orderBy="
                + getOrderBy() + ", optimizedMatcher=" + getOptimizedMatcher() + '}';
    }

    public String getObjKeyStartAfter() {
        return objKeyStartAfter;
    }

    public String getCommonPrefix() {
        return commonPrefix;
    }
}
