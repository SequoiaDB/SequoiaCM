package com.sequoiacm.cloud.adminserver.common;

public class StatisticsDefine {
    public static class StatisticsType {
        public static final int TRAFFIC = 1;
        public static final int FILE_DELTA = 2;
    }
    
    public static class InterfaceType {
        public static final String FILE_UPLOAD   = "file_upload";
        public static final String FILE_DOWNLOAD = "file_download";
    }
    
    public static String APPLICATION_PROPERTIES_LOCATION = "spring.config.location";
    public static String LOGGING_CONFIG = "logging.config";
    
    public static String DATE_FORMAT = "yyyyMMdd";
    
    public static final String METRICS_PREFIX_FILE_UPLOAD = "counter.file.upload.";
    public static final String METRICS_PREFIX_FILE_DOWNLOAD = "counter.file.download.";
    

    public static class Query {
        public static final String SEQUOIADB_MATCHER_IN = "$in";
        public static final String SEQUOIADB_MATCHER_GT = "$gt";
        public static final String SEQUOIADB_MATCHER_LT = "$lt";
        public static final String SEQUOIADB_MATCHER_ELEMMATCH = "$elemMatch";
        public static final String SEQUOIADB_MATCHER_AND = "$and";
    }

    public static class Modifier {
        public static final String SEQUOIADB_MODIFIER_SET = "$set";
        public static final String SEQUOIADB_MODIFIER_INC = "$inc";
    }
    
    public static final class DefaultValue {
        public static final String JOB_FIRST_TIME = "00:00:00";
        public static final String JOB_PERIOD = "1d";
        public static final String JOB_BREAKPOINT_FILE_CLEAN_PERIOD = "7d";
        public static final String JOB_BREAKPOINT_FILE_STAY_DAYS = "10d";
    }
    
    public static final class Scope {
        private Scope() {
        }
        public static final int SCOPE_CURRENT = 1;
        public static final int SCOPE_HISTORY = 2;
        public static final int SCOPE_ALL = 3;
    }
}
