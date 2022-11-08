package com.sequoiacm.s3import.common;

public class CommonDefine {

    public static final String SCM_OBJ_CREATE_TIME = "x-scm-object-create-time";

    public static class S3Client {
        // request headers
        public static final String AUTHORIZATION = "Authorization";
        public static final String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";

        public static final String SERVICE_NAME = "s3";
        public static final String DEFAULT_REGION = "us-east-1";
    }

    public static class Prop {
        public static final String SRC_S3_PREFIX = "src.s3.";
        public static final String DEST_S3_PREFIX = "dest.s3.";
        public static final String CONNECT_CONF_PREFIX = "client.";

        public static final String URL = "url";
        public static final String ACCESS_KEY = "accessKey";
        public static final String SECRET_KEY = "secretKey";
        public static final String KEY_FILE = "key-file";

        public static final String BATCH_SITE = "batch_size";
        public static final String MAX_FAIL_COUNT = "max_fail_count";
        public static final String WORK_COUNT = "work_count";
        public static final String STRICT_COMPARISON_MODE = "strict_comparison_mode";

        public static final String IGNORE_METADATA_PREFIX = "compare_ignore.metadata.";
    }

    public static class Option {
        public static final String LONG_HELP = "help";
        public static final String SHORT_HELP = "h";
        public static final String WORK_PATH = "work-path";
        public static final String CONF = "conf";
        public static final String MAX_EXEC_TIME = "max-exec-time";
        public static final String BUCKET = "bucket";
        public static final String RESET = "reset";
        public static final String CMP_RES_PATH = "cmp-result-path";
    }

    public static class Separator {
        public static final String BUCKET = ",";          // sfz,xyk,sbk
        public static final String BUCKET_DIFF_ARG = ":"; // sfz:scm-sfz,xyk,sbk
        public static final String BUCKET_DIFF = "_";     // sfz_scm-sfz,xyk,sbk
    }

    public static class ProgressStatus {
        public static final String INIT = "init";
        public static final String RUNNING = "running";
        public static final String FINISH = "finish";
    }

    public static class DiffType {
        public static final String SAME = "same";
        public static final String NEW = "new";
        public static final String DELETED = "deleted";
        public static final String DIFF = "diff";
        public static final String DIFF_VERSION = "diff_version";
        public static final String DIFF_METADATA = "diff_metadata";
        public static final String DIFF_CONTENT = "diff_content";
    }

    public static class SyncType {
        public static final String ADD = "add";
        public static final String DELETE = "delete";
        public static final String UPDATE = "update";
    }

    public static class LogConf {
        public static final String DEFAULT_FILE = "start.log";
        public static final String DEFAULT_FILE_NAME_PATTERN = "start.%i.log";
        public static final String FILE = "s3import.log";
        public static final String FILE_NAME_PATTERN = "s3import.%i.log";
    }

    public static class VersionConf {
        public static final String OFF = "Off";
        public static final String ENABLED = "Enabled";
        public static final String SUSPENDED = "Suspended";
    }

    public static class KeyMarker {
        public static final String BEGINNING = "";
        public static final String END = null;
    }

    public static class PathConstant {
        public static final String SYNC_ERROR = "sync_error";
        public static final String SYNC_FINISH = "sync_finish";
    }
}
