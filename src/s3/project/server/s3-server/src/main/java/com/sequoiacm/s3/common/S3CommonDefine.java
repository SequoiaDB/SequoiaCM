package com.sequoiacm.s3.common;

public class S3CommonDefine {

    public static final String NULL_VERSION_ID = "null";

    public static final String LIST_OBJECT_CONTEXT_TABLE_NAME = "SCMSYSTEM.S3_LIST_OBJECT_CONTEXT";
    public static final String LIST_OBJECT_CONTEXT_FIELD_TOKEN = "token";
    public static final String LIST_OBJECT_CONTEXT_FIELD_BUCKET_NAME = "bucket_name";
    public static final String LIST_OBJECT_CONTEXT_FIELD_PREFIX = "prefix";
    public static final String LIST_OBJECT_CONTEXT_FIELD_START_AFTER = "start_after";
    public static final String LIST_OBJECT_CONTEXT_FIELD_DELIMITER = "delimiter";
    public static final String LIST_OBJECT_CONTEXT_FIELD_LAST_ACCESS_TIME = "last_access_time";
    public static final String LIST_OBJECT_CONTEXT_FIELD_LAST_MARKER = "last_marker";

    public static final String DEFAULT_REGION_TABLE_NAME = "SCMSYSTEM.DEFAULT_REGION";
    public static final String DEFAULT_REGION_FIELD_WORKSPACE = "workspace";

    public static final String ID_GENERATOR_TABLE_NAME = "SCMSYSTEM.ID_GENERATOR";
    public static final String PARTS_TABLE_NAME = "SCMSYSTEM.S3_PARTS";
    public static final String UPLOAD_META_TABLE_NAME = "SCMSYSTEM.S3_UPLOAD_META";

    public static class UploadStatus {
        public static final int UPLOAD_INIT = 1;
        public static final int UPLOAD_COMPLETE = 2;
        public static final int UPLOAD_ABORT = 3;
    }

    public static class IdType {
        public static final String TYPE_UPLOAD = "s3_uploadId";
    }

    public static class PartNumberRange {
        // valid part number: [(1) , (10000)]
        // reserved part number: [(-1000) , (0)]
        // abandoned part number: [() - (-1000))
        public static final int VALID_PART_NUM_BEGIN = 1;
        public static final int RESERVED_PART_NUM_BEGIN = -1000;
        public static final int ABANDONED_PART_NUM_END = -1001;
    }
}
