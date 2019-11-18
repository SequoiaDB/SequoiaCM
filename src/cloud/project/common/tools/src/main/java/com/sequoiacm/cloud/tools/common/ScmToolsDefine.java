package com.sequoiacm.cloud.tools.common;

public class ScmToolsDefine {
    public static class FILE_NAME {
        // ************dir*****************
        public static final String CONF = "conf";
        public static final String LOG = "log";
        public static final String JARS = "jars";
        public static final String SERVICE_CENTER = "service-center";
        public static final String GATEWAY = "gateway";
        public static final String AUTH_SERVER = "auth-server";
        public static final String SERVICE_TRACE = "service-trace";
        public static final String ADMIN_SERVER = "admin-server";

        // *************file******************
        public static final String APP_PROPS = "application.properties";
        public static final String LOGBACK = "logback.xml";
        public static final String ERROR_OUT = "error.out";

        // jarName prefix
        public static final String SERVICE_CENTER_JAR_NAME_PREFIX = "sequoiacm-cloud-servicecenter-";
        public static final String GATEWAY_JAR_NAME_PREFIX = "sequoiacm-cloud-gateway-";
        public static final String AUTH_SERVER_JAR_NAME_PREFIX = "sequoiacm-cloud-authserver-";
        public static final String SERVICE_TRACE_JAR_NAME_PREFIX = "sequoiacm-cloud-servicetrace-";
        public static final String ADMIN_SERVER_JAR_NAME_PREFIX = "sequoiacm-cloud-adminserver-";

        // ***********tools log conf*************
        public static final String START_LOG_CONF = "logback_start.xml";
        public static final String STOP_LOG_CONF = "logback_stop.xml";

    }

    public static class PROPERTIES {
        public static final String SERVER_PORT = "server.port";
        public static final String JVM_OPTION = "scm.jvm.options";
        public static final String LOG_PATH_VALUE = "SCM_LOG_PATH_VALUE";
        public static final String LOG_NAME_VALUE = "SCM_LOG_NAME_VALUE";
        public static final String APPLICATION_PROPERTIES_LOCATION = "spring.config.location";
        public static final String LOGGING_CONFIG = "logging.config";

        public static final String LOG_AUDIT_SDB_URL = "SCM_AUDIT_SDB_URL";
        public static final String LOG_AUDIT_SDB_USER = "SCM_AUDIT_SDB_USER";
        public static final String LOG_AUDIT_SDB_PASSWD = "SCM_AUDIT_SDB_PASSWD";
    }

    public static class NODE_TYPE {
        public static final String SERVICE_CENTER_NUM = "1";
        public static final String SERVICE_CENTER_STR = FILE_NAME.SERVICE_CENTER;

        public static final String GATEWAY_NUM = "2";
        public static final String GATEWAY_STR = FILE_NAME.GATEWAY;

        public static final String AUTH_SERVER_NUM = "3";
        public static final String AUTH_SERVER_STR = FILE_NAME.AUTH_SERVER;

        public static final String SERVICE_TRACE_NUM = "20";
        public static final String SERVICE_TRACE_STR = FILE_NAME.SERVICE_TRACE;

        public static final String ADMIN_SERVER_NUM = "21";
        public static final String ADMIN_SERVER_STR = FILE_NAME.ADMIN_SERVER;

        // no enum
        public static final String ALL_NUM = "0";
        public static final String ALL_STR = "all";
    }
}
