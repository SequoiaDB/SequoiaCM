package com.sequoiacm.mappingutil.common;

public class CommonDefine {

    public static final String FILE_MATCHER_ALL = "all";

    public class MappingStatus {
        public static final String MAPPING = "mapping";
        public static final String FINISH = "finish";
    }

    public static class LogConf {
        public static final String DEFAULT_FILE = "start.log";
        public static final String DEFAULT_FILE_NAME_PATTERN = "start.%i.log";
        public static final String FILE = "fileMapping.log";
        public static final String FILE_NAME_PATTERN = "fileMapping.%i.log";
    }
}
