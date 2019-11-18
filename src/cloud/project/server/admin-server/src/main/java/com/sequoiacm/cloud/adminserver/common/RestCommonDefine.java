package com.sequoiacm.cloud.adminserver.common;

public class RestCommonDefine {

    public static final String CHARSET_UTF8 = "utf-8";

    /**
     * --------------request header------------------
     **/
    public static final String HOST = "Host";
    public static final String DATE = "Date";
    public static final String AUTHORIZATION = "Authorization";
    public static final String AUTHORIZATION_HEADER = "Scm ";

    /**
     * ---------------response header---------------------
     **/
    public static final String SERVER = "Server";
    public static final String X_SCM_REQUEST_ID = "X-SCM-Request-ID";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String X_SCM_COUNT = "X-SCM-Count";
    public static final String X_SCM_SUM = "X-SCM-Sum";

    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=utf-8";

    /**
     * -----------------------statistics---------------------------------
     **/
    public static final class RestArg {
        public static final String WORKSPACE_NAME   = "workspace_name";
        public static final String STATISTICS_TYPE  = "statistics_type";
        public static final String QUERY_FILTER = "filter";
        public static final String FILE_LIST_SCOPE = "scope";
    }
}
