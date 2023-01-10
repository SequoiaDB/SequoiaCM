package com.sequoiacm.om.omserver.common;

public class RestParamDefine {
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String GATEWAY_ADDR = "gateway_addr";
    public static final String REGION = "region";
    public static final String ZONE = "zone";
    public static final String SITE_ID = "site_id";
    public static final String SITE_NAME = "site_name";
    public static final String DATA_URL = "data_url";

    public static final String X_AUTH_TOKEN = "x-auth-token";

    public static final String X_RECORD_COUNT = "x-record-count";

    // COMMON
    public static final String LIMIT = "limit";
    public static final String SKIP = "skip";
    public static final String WORKSPACE = "workspace";
    public static final String FILE = "file";
    public static final String FILTER = "filter";
    public static final String ORDERBY = "orderby";
    public static final String STRICT_MODE = "strict_mode";

    // workspace
    public static final String BEGIN_TIME = "begin_time";
    public static final String END_TIME = "end_time";
    public static final String IS_FORCE = "is_force";

    // bucket
    public static final String BUCKET = "bucket";

    // user role
    public static final String USER_TYPE = "user_type";
    public static final String OLD_PASSWORD = "old_password";
    public static final String NEW_PASSWORD = "new_password";
    public static final String CLEAB_SESSIONS = "clean_sessions";
    public static final String ROLES = "roles";
    public static final String CONDITION = "condition";
    public static final String DESCRIPTION = "description";
    public static final String RESOURCE_TYPE = "resource_type";
    public static final String RESOURCE = "resource";
    public static final String PRIVILEGE = "privilege";

    // file
    public static final String MAJOR_VERSION = "major_version";
    public static final String MINOR_VERSION = "minor_version";
    public static final String FILE_LIST_SCOPE = "scope";
    public static final String FILE_DESCRIPTION = "description";
    public static final String FILE_UPLOAD_CONFIG = "upload_config";
    public static final String FILE_UPDATE_CONTENT_OPTION = "update_content_option";
    public static final String FILE_ID_LIST = "file_id_list";

    // directory
    public static final String DIRECTORY_TYPE_PATH = "path";
    public static final String DIRECTORY_TYPE_ID = "id";

    // schedule
    public static final String SCHEDULE_ID = "schedule_id";
    public static final String SCHEDULE_NAME = "name";
    public static final String SCHEDULE_TYPE = "type";
    public static final String SCHEDULE_WORKSPACE = "workspace";
    public static final String SCHEDULE_TRANSITION = "transition";
    public static final String SCHEDULE_SOURCE_SITE = "source_site";
    public static final String SCHEDULE_TARGET_SITE = "target_site";
    public static final String SCHEDULE_MAX_STAY_TIME = "max_stay_time";
    public static final String SCHEDULE_MAX_EXEC_TIME = "max_exec_time";
    public static final String SCHEDULE_SCOPE_TYPE = "scope_type";
    public static final String SCHEDULE_CRON = "cron";
    public static final String SCHEDULE_ENABLE = "enable";
    public static final String SCHEDULE_CREATE_TIME = "create_time";
    public static final String SCHEDULE_CREATE_USER = "create_user";
    public static final String SCHEDULE_CONTENT = "content";
    public static final String SCHEDULE_CONDITION = "condition";
    public static final String SCHEDULE_DESCRIPTION = "description";
    public static final String SCHEDULE_PREFERRED_REGION = "preferred_region";
    public static final String SCHEDULE_PREFERRED_ZONE = "preferred_zone";

    // lifecycle config
    public static final String STAGE_TAG = "stage_tag";
    public static final String STAGE_TAG_NAME = "name";
    public static final String STAGE_TAG_DESCRIPTION = "description";
    public static final String OLD_TRANSITION = "old_transition";
    public static final String TRANSITION = "transition";
    public static final String TRANSITION_ENABLED = "is_enabled";

    // service center
    public static final String SERVICE_REGION = "region";

    // action
    public static final String ACTION_STOP_TASK = "stop_task";

    public static final String IP_ADDR = "ip_addr";
    public static final String PORT = "port";
}
