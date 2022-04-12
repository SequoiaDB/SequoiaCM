package com.sequoiacm.s3.scan;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

public class BasicS3ScanMatcher implements S3ScanMatcher {
    private String field;
    private String prefix;
    BSONObject additionalMatcher;

    public BasicS3ScanMatcher(String field, String prefix, BSONObject additionalMatcher) {
        this.field = field;
        if (prefix != null && prefix.length() <= 0) {
            prefix = null;
        }
        this.prefix = prefix;
        this.additionalMatcher = additionalMatcher;
    }

    @Override
    public BSONObject getMatcher() {
        BasicBSONList andArr = new BasicBSONList();

        if (prefix != null) {
            BSONObject prefixMatcher = new BasicBSONObject();
            prefixMatcher.put("$gte", prefix);
            String prefixEnd = prefix.substring(0, prefix.length() - 1)
                    + (char) (prefix.charAt(prefix.length() - 1) + 1);
            prefixMatcher.put("$lt", prefixEnd);
            andArr.add(new BasicBSONObject(field, prefixMatcher));
        }

        if (additionalMatcher != null) {
            andArr.add(additionalMatcher);
        }

        if (andArr.isEmpty()) {
            return null;
        }
        return new BasicBSONObject("$and", andArr);
    }

    @Override
    public String toString() {
        return "BasicS3ScanMatcher{" + "prefix='" + prefix + '\'' + ", additionalMatcher="
                + additionalMatcher + '}';
    }
}
