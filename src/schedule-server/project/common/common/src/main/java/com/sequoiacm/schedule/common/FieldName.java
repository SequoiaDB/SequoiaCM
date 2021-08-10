package com.sequoiacm.schedule.common;

public class FieldName {
    public static class File {
        // array, [{site_id:1, create_time:123213121, last_access_time:148523}]
        public static final String FIELD_CLFILE_FILE_SITE_LIST = "site_list";
        // int last read content's time
        public static final String FIELD_CLFILE_FILE_SITE_LIST_ID = "site_id";
        // long last create content's time
        public static final String FIELD_CLFILE_FILE_SITE_LIST_TIME = "last_access_time";
        // long
        public static final String FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME = "create_time";

        public static final String FIELD_CLFILE_FILE_DATA_ID = "data_id";
        public static final String FIELD_CLFILE_FILE_DATA_TYPE = "data_type";
        public static final String FIELD_CLFILE_FILE_SIZE = "size"; //long,  1
        public static final String FIELD_CLFILE_FILE_DATA_CREATE_TIME = "data_create_time"; //long, 1485239715515(ms)

        public static final String FIELD_CLFILE_ID = "id"; //string, fileid
        public static final String FIELD_CLFILE_MAJOR_VERSION = "major_version"; //int, 1
        public static final String FIELD_CLFILE_MINOR_VERSION = "minor_version"; //int, 0
        public static final String FIELD_CLFILE_INNER_CREATE_MONTH = "create_month"; //string 201701

    }

    public static class Task {
        public static final String FIELD_ID = "id";
        public static final String FIELD_TYPE = "type";
        public static final String FIELD_WORKSPACE = "workspace_name";
        public static final String FIELD_CONTENT = "content";
        public static final String FIELD_SERVER_ID = "server_id";
        public static final String FIELD_PROGRESS = "progress";
        public static final String FIELD_RUNNING_FLAG = "running_flag";
        public static final String FIELD_DETAIL = "detail";
        public static final String FIELD_START_TIME = "start_time";
        public static final String FIELD_STOP_TIME = "stop_time";
        public static final String FIELD_ESTIMATE_COUNT = "estimate_count";
        public static final String FIELD_ACTUAL_COUNT = "actual_count";
        public static final String FIELD_SUCCESS_COUNT = "success_count";
        public static final String FIELD_FAIL_COUNT = "fail_count";
        public static final String FIELD_SCHEDULE_ID = "schedule_id";
        public static final String FIELD_TARGET_SITE = "target_site";
        public static final String FIELD_SCOPE = "scope";
        public static final String FIELD_MAX_EXEC_TIME = "max_exec_time";
    }

    public static class ScheduleStatus {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_WORKER_NODE = "worker_node";
        public static final String FIELD_START_TIME = "start_time";
        public static final String FIELD_STATUS = "status";
    }

    public static class Schedule {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_DESC = "desc";
        // string
        public static final String FIELD_TYPE = "type";
        // string
        public static final String FIELD_WORKSPACE = "workspace";
        // bson
        public static final String FIELD_CONTENT = "content";
        // string
        public static final String FIELD_CRON = "cron";
        // string
        public static final String FIELD_CREATE_USER = "create_user";
        // long (ms)
        public static final String FIELD_CREATE_TIME = "create_time";
        // boolean
        public static final String FIELD_ENABLE = "enable";

        //int scope type
        public static final String FIELD_SCOPE = "scope";

        public static final String FIELD_MAX_EXEC_TIME = "max_exec_time";

        // string (ex. '3d')
        public static final String FIELD_MAX_STAY_TIME = "max_stay_time";
        // bson
        public static final String FIELD_EXTRA_CONDITION = "extra_condition";

        public static final String FIELD_PREFERRED_REGION = "preferred_region";
        public static final String FIELD_PREFERRED_ZONE = "preferred_zone";

        // ***************** clean job *****************
        public static final String FIELD_CLEAN_SITE = "site";
        // for inner use
        public static final String FIELD_CLEAN_SITE_ID = "site_id";
        // *********************************************

        // ***************** copy job *****************
        public static final String FIELD_COPY_SOURCE_SITE = "source_site";
        // for inner use
        public static final String FIELD_COPY_SOURCE_SITE_ID = "source_site_id";

        public static final String FIELD_COPY_TARGET_SITE = "target_site";
        // for inner use
        public static final String FIELD_COPY_TARGET_SITE_ID = "target_site_id";
        // *********************************************

        // ***************** internal job *****************
        public static final String FIELD_INTERNAL_JOB_DATA = "job_data";
        public static final String FIELD_INTERNAL_JOB_TYPE = "job_type";
        public static final String FIELD_INTERNAL_WORKER_SERVICE = "worker_service";
        public static final String FIELD_INTERNAL_WORKER_NODE = "worker_node";
        public static final String FIELD_INTERNAL_WORKER_START_TIME = "worker_start_time";
        public static final String FIELD_INTERNAL_WORKER_PREFER_ZONE = "worker_PREFER_ZONE";
        public static final String FIELD_INTERNAL_WORKER_PREFER_REGION = "worker_PREFER_REGION";
        // *********************************************

        // for datamap key
        public static final String FIELD_INTERNAL_SCH_INFO = "internal_sch_info";
        public static final String FIELD_SCH_INFO = "sch_info";
        public static final String FIELD_DISCOVER_CLIENT = "discover_client";
        public static final String FIELD_FEIGN_CLIENT_FACTORY = "feign_client_factory";
        public static final String FIELD_SCHEDULE_DAO = "schedule_dao";

    }

    public static class Site {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_ROOT_FLAG = "root_site_flag";
    }

    public static class Workspace {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_DATA_LOCATION = "data_location";
        public static final String FIELD_SITE_ID = "site_id";
    }

    public static class FileServer {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_SITE_ID = "site_id";
        public static final String FIELD_HOSTNAME = "host_name";
        public static final String FIELD_PORT = "port";
    }

    public static final class Strategy {
        private Strategy() {
        }

        public static final String FIELD_SOURCE_SITE = "source_site";
        public static final String FIELD_TARGET_SITE = "target_site";
        public static final String FIELD_CONNECTIVITY = "connectivity";
    }
}
