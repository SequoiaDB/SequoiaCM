package com.sequoiacm.s3.scan;

import org.bson.BSONObject;

import com.sequoiacm.s3.exception.S3ServerException;

public class BasicS3ScanCommonPrefixParser implements S3ScanCommonPrefixParser {
    private final int prefixLength;
    private String field;
    private String delimiter;

    public BasicS3ScanCommonPrefixParser(String field, String delimiter, String prefix) {
        this.field = field;
        if (delimiter != null && delimiter.length() <= 0) {
            delimiter = null;
        }

        this.delimiter = delimiter;
        this.prefixLength = prefix == null ? 0 : prefix.length();
    }

    @Override
    public String getCommonPrefix(BSONObject record) throws S3ServerException {
        if (delimiter == null) {
            return null;
        }

        String prefixFieldValue = (String) record.get(field);
        return getCommonPrefix(prefixFieldValue);
    }

    public String getCommonPrefix(String key) {
        if (key == null || delimiter == null) {
            return null;
        }
        int delimiterIndex = key.indexOf(delimiter, prefixLength);
        if (delimiterIndex != -1) {
            return key.substring(0, delimiterIndex + delimiter.length());
        }
        return null;
    }

    @Override
    public String toString() {
        return "BasicS3ScanDelimiter{" + "prefixLength=" + prefixLength + ", field='" + field + '\''
                + ", delimiter='" + delimiter + '\'' + '}';
    }
}
