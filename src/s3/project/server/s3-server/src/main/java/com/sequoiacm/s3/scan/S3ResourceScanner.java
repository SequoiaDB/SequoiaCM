package com.sequoiacm.s3.scan;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.exception.S3ServerException;

public class S3ResourceScanner {
    private final S3ScanMatcher matcher;
    private final S3ScanOffset offset;
    private final S3ScanCommonPrefixParser delimiter;
    private final int limit;
    private final S3ScanRecordCursorProvider cursorProvider;

    public S3ResourceScanner(MetaAccessor metaAccessor,
                             S3ScanMatcher matcher, S3ScanOffset offset, S3ScanCommonPrefixParser delimiter, int limit) {
        this.cursorProvider = new BasicS3ScanRecordCursorProvider(metaAccessor);
        this.matcher = matcher;
        this.offset = offset;
        this.delimiter = delimiter;
        this.limit = limit;
    }

    public S3ResourceScanner(S3ScanRecordCursorProvider cursorProvider, S3ScanMatcher matcher,
                             S3ScanOffset offset, S3ScanCommonPrefixParser delimiter, int limit) {
        this.cursorProvider = cursorProvider;
        this.matcher = matcher;
        this.offset = offset;
        this.delimiter = delimiter;
        this.limit = limit;
    }

    public S3ScanResult doScan()
            throws S3ServerException, ScmMetasourceException {
        S3ScanResult result = new S3ScanResult();
        BasicBSONObject queryMatcher = buildQueryMatcher();
        RecordWrapperCursor<? extends RecordWrapper> cursor = null;
        try {
            cursor = cursorProvider.createRecordCursor(queryMatcher, offset.getOrderBy());
            RecordWrapper lastRecordWrapper = null;
            while (cursor.hasNext() && result.getSize() < limit + 1) {
                RecordWrapper recordWrapper = cursor.getNext();
                if (!offset.isReach(recordWrapper.getRecord())) {
                    continue;
                }
                if (result.getSize() == limit) {
                    // 第 limit + 1 条不做为结果集，只用于置位 isTruncate
                    String commonPrefix = delimiter.getCommonPrefix(recordWrapper.getRecord());
                    if (commonPrefix != null && commonPrefix.equals(result.getLastCommonPrefix())) {
                        // 这条记录是 commonPrefix，并且已经存在于结果集中，所以需要继续查看下一条记录，来确定是否置位 isTruncate
                        continue;
                    }
                    result.setTruncated(true);
                    break;
                }

                lastRecordWrapper = recordWrapper;
                String commonPrefix = delimiter.getCommonPrefix(recordWrapper.getRecord());
                if (commonPrefix != null) {
                    result.addCommonPrefix(commonPrefix);
                    continue;
                }
                result.addContent(recordWrapper);
            }
            if (!result.isTruncated()) {
                return result;
            }


            if (lastRecordWrapper == null) {
                // isTruncated 是true，lastRecord又是null，说明给的 limit == 0
                result.setNextScanOffset(offset);
            }
            else {
                result.setNextScanOffset(offset.createOffsetByRecord(lastRecordWrapper.getRecord(),
                        delimiter.getCommonPrefix(lastRecordWrapper.getRecord())));
            }
            return result;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private BasicBSONObject buildQueryMatcher() {
        BasicBSONList andArr = new BasicBSONList();
        BSONObject matcherBson = matcher.getMatcher();
        if (matcherBson != null) {
            andArr.add(matcherBson);
        }
        if (offset.getOptimizedMatcher() != null) {
            andArr.add(offset.getOptimizedMatcher());
        }
        if (andArr.size() > 0) {
            return new BasicBSONObject("$and", andArr);
        }
        return null;
    }
}
