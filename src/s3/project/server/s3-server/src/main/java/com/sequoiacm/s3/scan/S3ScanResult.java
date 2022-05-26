package com.sequoiacm.s3.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BSONObject;

public class S3ScanResult {
    private List<RecordWrapper> content = new ArrayList<>();
    private TreeSet<String> commonPrefixSet = new TreeSet<>();
    private boolean isTruncated = false;

    private S3ScanOffset nextScanOffset;

    public int getSize() {
        return content.size() + commonPrefixSet.size();
    }

    void addContent(RecordWrapper r) {
        content.add(r);
    }

    void addCommonPrefix(String commonPrefix) {
        commonPrefixSet.add(commonPrefix);
    }

    void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public S3ScanOffset getNextScanOffset() {
        return nextScanOffset;
    }

    void setNextScanOffset(S3ScanOffset nextScanOffset) {
        this.nextScanOffset = nextScanOffset;
    }

    public List<RecordWrapper> getContent() {
        return content;
    }

    public Set<String> getCommonPrefixSet() {
        return commonPrefixSet;
    }

    String getLastCommonPrefix() {
        if (commonPrefixSet.size() <= 0) {
            return null;
        }
        return commonPrefixSet.last();
    }
}
