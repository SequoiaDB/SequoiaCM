package com.sequoiacm.schedule.common;

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

    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=utf-8";

    /**
     * ---------------error code----------------------------
     **/

    public static class ErrorCode {
        public static final String INVALID_AUTH_INFO = "invalid_auth_info";
        public static final String PERMISSION_DENIED = "permission_denied";
        public static final String INVALID_ACCOUNT = "invalid_account";
        public static final String RECORD_NOT_EXISTS = "record_not_exists";
        public static final String ROOT_SITE_NOT_EXISTS = "root_site_not_exists";
        public static final String SITE_NOT_EXISTS = "site_not_exists";
        public static final String WORKSPACE_NOT_EXISTS = "workspace_not_exists";
        public static final String FILE_NOT_EXISTS = "file_not_exists";
        public static final String TASK_NOT_EXISTS = "task_not_exists";
        public static final String INVALID_ARGUMENT = "invalid_argument";
        public static final String MISSING_ARGUMENT = "missing_argument";
        public static final String INTERNAL_ERROR = "internal_error";
        public static final String BAD_REQUEST = "bad_request";
        public static final String WORKER_SHOULD_STOP = "worker_should_stop";
    }

    /**
     * -----------------------file---------------------------------
     **/
    public static final int TRANSMISSION_LEN = 1024 * 1024;

    public static class RestParam {
        public static final String KEY_ERROR_RES_CODE = "code";
        public static final String KEY_ERROR_RES_MESSAGE = "message";
        public static final String KEY_ERROR_RES_LOCATION = "location";

        public static final String KEY_DESCRIPTION = "description";
        public static final String KEY_NOTIFY_TYPE = "notify_type";
        public static final String KEY_QUERY_FILTER = "filter";

        public static final int VALUE_NOTIFY_TYPE_START = 1;
        public static final int VALUE_NOTIFY_TYPE_STOP = 2;

        public static final String KEY_STOP_WORKER = "stop_worker";

        public static final String STOP_WORKER = "stop_worker";
        public static final String REST_SCHEDULE_ID = "schedule_id";
        public static final String REST_SCHEDULE_NAME = "schedule_name";
        public static final String REST_START_TIME = "start_time";
        public static final String REST_JOB_DATA = "job_data";
        public static final String REST_JOB_TYPE = "job_type";
        public static final String REST_WORKER_NODE = "worker_node";
        public static final String REST_WORKER_STATUS = "status";
        public static final String REST_WORKER_IS_FINISH = "is_finish";
    }
}
