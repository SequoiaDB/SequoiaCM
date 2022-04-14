package com.sequoiacm.s3.scan;

import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class BasicS3ScanOffset implements S3ScanOffset {
    private String majorKeyField;
    private String majorKeyStartAfter;
    private String minorKeyField;
    private boolean minorKeyAscendingOrder = true;
    private Comparable minorKeyStartAfter;
    private boolean hasReach = false;
    private String commonPrefix;

    public BasicS3ScanOffset(String majorKeyField, String majorKeyStartAfter, String commonPrefix)
            throws S3ServerException {
        this(majorKeyField, majorKeyStartAfter, null, null, commonPrefix, true);
    }

    public BasicS3ScanOffset(String majorKeyField, String majorKeyStartAfter, String minorKeyField,
            Comparable minorKeyStartAfter, String commonPrefix, boolean minorKeyAscendingOrder)
            throws S3ServerException {
        this.majorKeyField = majorKeyField;
        if (majorKeyStartAfter != null && majorKeyStartAfter.length() <= 0) {
            majorKeyStartAfter = null;
        }
        this.majorKeyStartAfter = majorKeyStartAfter;
        this.minorKeyField = minorKeyField;
        this.minorKeyStartAfter = minorKeyStartAfter;
        this.commonPrefix = commonPrefix;

        if (commonPrefix != null) {
            if (majorKeyStartAfter == null) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "majorKeyStartAfter must be not null when commonPrefix is not null: commonPrefix="
                                + commonPrefix);
            }
            if (!majorKeyStartAfter.startsWith(commonPrefix)) {
                throw new S3ServerException(S3Error.SYSTEM_ERROR,
                        "majorKeyStartAfter must start with commonPrefix: majorKeyStartAfter="
                                + majorKeyStartAfter + ", commonPrefix=" + commonPrefix);
            }
        }

        this.minorKeyAscendingOrder = minorKeyAscendingOrder;
    }

    @Override
    public boolean isReach(BSONObject record) {
        if (hasReach) {
            return true;
        }

        if (majorKeyStartAfter == null) {
            hasReach = true;
            return true;
        }

        String majorKey = (String) record.get(majorKeyField);
        if (commonPrefix != null) {
            // 本 offset 代表的是一个 commonPrefix，且这个 commonPrefix之前已经给过客户端了，
            // 这里检查记录是否也是这个commonPrefix，如果是就越过它
            if (majorKey.startsWith(commonPrefix)) {
                return false;
            }
            hasReach = true;
            return true;
        }

        int majorKeyCompareResult = majorKeyStartAfter.compareTo(majorKey);
        if (majorKeyCompareResult > 0) {
            return false;
        }
        if (majorKeyCompareResult == 0) {
            if (hasSpecifyMinorKey()) {
                Object minorKey = record.get(minorKeyField);
                int compare = minorKeyStartAfter.compareTo(minorKey);
                if (minorKeyAscendingOrder) {
                    if (compare >= 0) {
                        return false;
                    }
                }
                else {
                    if (compare <= 0) {
                        return false;
                    }
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
        orderBy.put(majorKeyField, 1);
        if (minorKeyField != null) {
            if (minorKeyAscendingOrder) {
                orderBy.put(minorKeyField, 1);
            }
            else {
                orderBy.put(minorKeyField, -1);
            }
        }
        return orderBy;
    }

    @Override
    public BSONObject getOptimizedMatcher() {
        BasicBSONObject majorKeyMatcher = new BasicBSONObject();
        if (majorKeyStartAfter != null) {
            if (hasSpecifyMinorKey()) {
                majorKeyMatcher.put("$gte", majorKeyStartAfter);
            }
            else {
                majorKeyMatcher.put("$gt", majorKeyStartAfter);
            }
            return new BasicBSONObject(majorKeyField, majorKeyMatcher);
        }
        return null;

    }

    boolean hasSpecifyMinorKey() {
        return minorKeyField != null && minorKeyStartAfter != null;
    }

    @Override
    public S3ScanOffset createOffsetByRecord(BSONObject b, String commonPrefix)
            throws S3ServerException {
        if (minorKeyField != null) {
            return new BasicS3ScanOffset(majorKeyField, (String) b.get(majorKeyField),
                    minorKeyField, (Comparable) b.get(minorKeyField), commonPrefix,
                    minorKeyAscendingOrder);
        }
        return new BasicS3ScanOffset(majorKeyField, (String) b.get(majorKeyField), commonPrefix);
    }

    @Override
    public String toString() {
        return "BasicS3ScanOffset{" + "majorKeyStartAfter=" + majorKeyStartAfter
                + ", minorKeyStartAfter=" + minorKeyStartAfter + '}';
    }

    public String getMajorKeyStartAfter() {
        return majorKeyStartAfter;
    }

    public Comparable getMinorKeyStartAfter() {
        return minorKeyStartAfter;
    }

    public String getCommonPrefix() {
        return commonPrefix;
    }
}
