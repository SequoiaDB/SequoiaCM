package com.sequoiacm.s3.scan;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.exception.S3ServerException;

public class S3ResourceScanner {
    private final S3ScanMatcher matcher;
    private final S3ScanOffset offset;
    private final S3ScanCommonPrefixParser delimiter;
    private final int limit;
    private final MetaAccessor metaAccessor;

    public S3ResourceScanner(MetaAccessor metaAccessor,
                             S3ScanMatcher matcher, S3ScanOffset offset, S3ScanCommonPrefixParser delimiter, int limit) {
        this.metaAccessor = metaAccessor;
        this.matcher = matcher;
        this.offset = offset;
        this.delimiter = delimiter;
        this.limit = limit;
    }

    public S3ScanResult doScan()
            throws S3ServerException, ScmServerException, ScmMetasourceException {
        S3ScanResult result = new S3ScanResult();
        BasicBSONObject queryMatcher = buildQueryMatcher();
        MetaCursor cursor = null;
        try {
            cursor = metaAccessor.query(queryMatcher, null, offset.getOrderBy(), 0, -1);
            BSONObject lastRecord = null;
            while (cursor.hasNext() && result.getSize() < limit + 1) {
                BSONObject record = cursor.getNext();
                if (!offset.isReach(record)) {
                    continue;
                }
                if (result.getSize() == limit) {
                    // 第 limit + 1 条不做为结果集，只用于置位 isTruncate
                    String commonPrefix = delimiter.getCommonPrefix(record);
                    if (commonPrefix != null && commonPrefix.equals(result.getLastCommonPrefix())) {
                        // 这条记录是 commonPrefix，并且已经存在于结果集中，所以需要继续查看下一条记录，来确定是否置位 isTruncate
                        continue;
                    }
                    result.setTruncated(true);
                    break;
                }

                lastRecord = record;
                String commonPrefix = delimiter.getCommonPrefix(record);
                if (commonPrefix != null) {
                    result.addCommonPrefix(commonPrefix);
                    continue;
                }
                result.addContent(record);
            }
            if (!result.isTruncated()) {
                return result;
            }


            if (lastRecord == null) {
                // isTruncated 是true，lastRecord又是null，说明给的 limit == 0
                result.setNextScanOffset(offset);
            }
            else {
                result.setNextScanOffset(offset.createOffsetByRecord(lastRecord,
                        delimiter.getCommonPrefix(lastRecord)));
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
