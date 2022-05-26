package com.sequoiacm.s3.scan;

import org.bson.BSONObject;

public class ListVersionRecordWrapper extends RecordWrapper {
    private final boolean isLatestVersion;

    public ListVersionRecordWrapper(BSONObject record, boolean isLatestVersion) {
        super(record);
        this.isLatestVersion = isLatestVersion;
    }

    public boolean isLatestVersion() {
        return isLatestVersion;
    }
}
