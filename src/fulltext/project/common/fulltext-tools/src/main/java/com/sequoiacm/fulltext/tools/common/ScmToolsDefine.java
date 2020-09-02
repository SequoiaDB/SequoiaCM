package com.sequoiacm.fulltext.tools.common;

public class ScmToolsDefine {
    public static final String SCM_JVM_OPTIONS = "scm.jvm.options";

    public static class FILE_NAME {
        // ************dir*****************
        public static final String CONF = "conf";
        public static final String LOG = "log";
        public static final String JARS = "jars";
        public static final String FULLTEXT_SERVER = "fulltext-server";

        // *************file******************
        public static final String APP_PROPS = "application.properties";
        public static final String LOGBACK = "logback.xml";
        public static final String ERROR_OUT = "error.out";

        // jarName prefix
        public static final String FULLTEXT_SERVER_JAR_NAME_PREFIX = "sequoiacm-fulltext-server-";

        // ***********tools log conf*************
        public static final String START_LOG_CONF = "logback_start.xml";
        public static final String STOP_LOG_CONF = "logback_stop.xml";

    }

    public static class PROPERTIES {
        public static final String SERVER_PORT = "server.port";
        public static final String LOG_PATH_VALUE = "SCM_LOG_PATH_VALUE";
        public static final String LOG_NAME_VALUE = "SCM_LOG_NAME_VALUE";
        public static final String APPLICATION_PROPERTIES_LOCATION = "spring.config.location";
        public static final String LOGGING_CONFIG = "logging.config";
    }

    public static class NODE_TYPE {
        public static final String FULLTEXT_SERVER_NUM = "1";
        public static final String FULLTEXT_SERVER_STR = FILE_NAME.FULLTEXT_SERVER;

        // no enum
        public static final String ALL_NUM = "0";
        public static final String ALL_STR = "all";
    }
}
