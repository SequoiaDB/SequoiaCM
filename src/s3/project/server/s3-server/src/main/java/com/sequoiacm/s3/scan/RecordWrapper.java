package com.sequoiacm.s3.scan;

import org.bson.BSONObject;

public class RecordWrapper {
    private BSONObject record;

    public RecordWrapper(BSONObject record) {
        this.record = record;
    }

    public BSONObject getRecord() {
        return record;
    }
}
