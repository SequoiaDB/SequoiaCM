package com.sequoiacm.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class FieldName {
    public static final String FIELD_ALL_OBJECTID = "_id"; // objectid

    // CL_SITE
    public static final String FIELD_CLSITE_NAME = "name"; // string, site1
    public static final String FIELD_CLSITE_ID = "id"; // int, 1
    public static final String FIELD_CLSITE_MAINFLAG = "root_site_flag"; // bool
                                                                         // false
    public static final String FIELD_CLSITE_STAGE_TAG = "stage_tag";

    // CL_SITE.META || CL_SITE.DATA
    public static final String FIELD_CLSITE_META = "meta"; // meta:{xxx}
    public static final String FIELD_CLSITE_DATA = "data"; // data:{xxx}

    public static final String FIELD_CLSITE_DATA_TYPE = "type"; // type:sequoiadb/hbase
    public static final String FIELD_CLSITE_URL = "url"; // array,
                                                         // ["192.168.20.56:50000"]
    public static final String FIELD_CLSITE_USER = "user";
    public static final String FIELD_CLSITE_PASSWD_TYPE = "password_type";
    public static final String FIELD_CLSITE_PASSWD = "password";
    public static final String FIELD_CLSITE_CONF = "configuration"; // for
                                                                    // hdfs|hbase

    // CL_CONTENTSERVER
    public static final String FIELD_CLCONTENTSERVER_NAME = "name"; // string,
                                                                    // contentserver1
    public static final String FIELD_CLCONTENTSERVER_SITEID = "site_id"; // int,
                                                                         // 1
    public static final String FIELD_CLCONTENTSERVER_HOST_NAME = "host_name"; // string,
                                                                              // host1
    public static final String FIELD_CLCONTENTSERVER_PORT = "port"; // int,
                                                                    // 12345
    public static final String FIELD_CLCONTENTSERVER_TYPE = "type"; // int, 1
                                                                    // metadata;
                                                                    // 2 lob
    public static final String FIELD_CLCONTENTSERVER_ID = "id";

    // CL_WORKSPACE
    public static final String FIELD_CLWORKSPACE_NAME = "name"; // string, w1
    public static final String FIELD_CLWORKSPACE_META_LOCATION = "meta_location"; // object,
                                                                                  // {
                                                                                  // "site_id":1,
                                                                                  // "domain":"domain1"
                                                                                  // }
    public static final String FIELD_CLWORKSPACE_DATA_LOCATION = "data_location"; // array,
                                                                                  // [{"site_id":1,
                                                                                  // "domain":"domain2"}
    public static final String FIELD_CLWORKSPACE_DATA_CEPHS3_USER_INFO = "user_info";
    public static final String FIELD_CLWORKSPACE_DATA_CEPHS3_PRIMARY = "primary";
    public static final String FIELD_CLWORKSPACE_DATA_CEPHS3_STANDBY = "standby";
    public static final String FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_USER = "user";
    public static final String FIELD_CLWORKSPACE_DATA_CEPHS3_CONFIG_PASSWORD = "password";
    public static final String FIELD_CLWORKSPACE_LOCATION_DOMAIN = "domain";
    public static final String FIELD_CLWORKSPACE_LOCATION_SITE_ID = "site_id";
    public static final String FIELD_CLWORKSPACE_ID = "id"; // int, 1
    public static final String FIELD_CLWORKSPACE_META_SHARDING_TYPE = "meta_sharding_type";
    public static final String FIELD_CLWORKSPACE_DATA_SHARDING_TYPE = "data_sharding_type";
    public static final String FIELD_CLWORKSPACE_OBJECT_SHARDING_TYPE = "object_sharding_type";
    public static final String FIELD_CLWORKSPACE_DATA_CS = "collection_space";
    public static final String FIELD_CLWORKSPACE_DATA_CL = "collection";
    public static final String FIELD_CLWORKSPACE_META_CS = "collection_space";
    public static final String FIELD_CLWORKSPACE_META_CL = "collection";
    public static final String FIELD_CLWORKSPACE_DATA_OPTIONS = "data_options";
    public static final String FIELD_CLWORKSPACE_META_OPTIONS = "meta_options";
    public static final String FIELD_CLWORKSPACE_EXT_DATA = "external_data";
    public static final String FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK = "tag_index_task_id";
    public static final String FIELD_CLWORKSPACE_EXT_DATA_TAG_IDX_TASK_ERROR = "tag_index_task_error";
    public static final String FIELD_CLWORKSPACE_BATCH_SHARDING_TYPE = "batch_sharding_type";
    public static final String FIELD_CLWORKSPACE_BATCH_ID_TIME_REGEX = "batch_id_time_regex";
    public static final String FIELD_CLWORKSPACE_BATCH_ID_TIME_PATTERN = "batch_id_time_pattern";
    public static final String FIELD_CLWORKSPACE_BATCH_FILE_NAME_UNIQUE = "batch_file_name_unique";
    public static final String FIELD_CLWORKSPACE_ENABLE_DIRECTORY = "enable_directory";
    public static final String FIELD_CLWORKSPACE_PREFERRED = "preferred";
    public static final String FIELD_CLWORKSPACE_SITE_CACHE_STRATEGY = "site_cache_strategy";
    public static final String FIELD_CLWORKSPACE_CONTAINER_PREFIX = "container_prefix"; // only
    public static final String FIELD_CLWORKSPACE_BUCKET_NAME = "bucket_name"; // in
                                                                              // s3
                                                                              // location
                                                                              // now
    public static final String FIELD_CLWORKSPACE_DATA_PATH = "data_path"; // sftp
    public static final String FIELD_CLWORKSPACE_DESCRIPTION = "description";
    public static final String FIELD_CLWORKSPACE_CREATEUSER = "create_user";
    public static final String FIELD_CLWORKSPACE_CREATETIME = "create_time";
    public static final String FIELD_CLWORKSPACE_UPDATEUSER = "update_user";
    public static final String FIELD_CLWORKSPACE_UPDATETIME = "update_time";
    public static final String FIELD_CLWORKSPACE_VERSION = "version"; // int
    // hbase location
    // public static final String FIELD_CLWORKSPACE_HBASE_CLIENT_PAUSE =
    // "hbase.client.pause";
    // public static final String FIELD_CLWORKSPACE_HBASE_CLIENT_RETRIES_NUMBER
    // = "hbase.client.retries.number";
    // public static final String FIELD_CLWORKSPACE_HBASE_RPC_TIMEOUT =
    // "hbase.rpc.timeout";
    // public static final String
    // FIELD_CLWORKSPACE_HBASE_CLIENT_OPERATION_TIMEOUT =
    // "hbase.client.operation.timeout";
    // public static final String
    // FIELD_CLWORKSPACE_HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD =
    // "hbase.client.scanner.timeout.period";
    public static final String FIELD_CLWORKSPACE_HABSE_NAME_SPACE = "hbase_namespace";

    // hdfs location
    public static final String FIELD_CLWORKSPACE_HDFS_DFS_ROOT_PATH = "hdfs_file_root_path";

    public static final String FIELD_CLWORKSPACE_EXTRA_META_CS = "extra_meta_cs";

    public static final String FIELD_CLWORKSPACE_TAG_LIB_META_OPTION = "tag_lib_meta_option";
    public static final String FIELD_CLWORKSPACE_TAG_LIB_META_OPTION_DOMAIN = "tag_lib_domain";

    public static final String FIELD_CLWORKSPACE_TAG_LIB_TABLE = "tag_lib_table";
    public static final String FIELD_CLWORKSPACE_TAG_RETRIEVAL_STATUS = "tag_retrieval_status";
    public static final String FIELD_CLWORKSPACE_TAG_UPGRADING = "tag_upgrading";

    // CL_USER
    public static final String FIELD_CLUSER_USERNAME = "user"; // string, user1
    public static final String FIELD_CLUSER_PASSWORD = "password"; // string,
                                                                   // passwd1
    // CL_ROLE
    public static final String FIELD_CLROLE_ROLENAME = "roleName"; // string,
    public static final String FIELD_CLROLE_DESCRIPTION = "description"; // string,

    // CL_SESSION
    public static final String FIELD_CLSESSION_ID = "id"; // string, session1
    public static final String FIELD_CLSESSION_USER = FIELD_CLUSER_USERNAME; // string,
                                                                             // user1
    public static final String FIELD_CLSESSION_LOGIN_SERVER = "login_server"; // string,
                                                                              // contentserver1
    public static final String FIELD_CLSESSION_LOGIN_TIME = "login_time"; // time,
                                                                          // 2017-01-10-10.10.10.123456
    public static final String FIELD_CLSESSION_LASTACTIVE_TIME = "last_active_time"; // time,
                                                                                     // 2017-01-10-10.10.10.123456
    public static final String FIELD_CLSESSION_ACCESSIP = "access_host_info"; // string,
                                                                              // host1:1234

    // CL_FILE & CL_FILE_HISTORY (file & document)
    public static final String FIELD_CLFILE_ID = "id"; // string, fileid
    public static final String FIELD_CLFILE_NAME = "name"; // string, filename
    public static final String FIELD_CLFILE_MAJOR_VERSION = "major_version"; // int,
                                                                             // 1
    public static final String FIELD_CLFILE_MINOR_VERSION = "minor_version"; // int,
                                                                             // 0
    public static final String FIELD_CLFILE_TYPE = "type"; // int, 1 file; 2
                                                           // document
    public static final String FIELD_CLFILE_BATCH_ID = "batch_id"; // string,
                                                                   // batch1
    public static final String FIELD_CLFILE_DIRECTORY_ID = "directory_id"; // string,
                                                                           // directory1
    public static final String FIELD_CLFILE_PROPERTIES = "class_properties"; // array,
                                                                             // [{key:value}]
    public static final String FIELD_CLFILE_TAGS = "tags"; // array,
                                                           // [{key:value}]

    // CL_FILE & CL_FILE_HISTORY INNER inner attribute(file & document)
    public static final String FIELD_CLFILE_INNER_USER = "user"; // string, user
    public static final String FIELD_CLFILE_INNER_CREATE_TIME = "create_time"; // long,
                                                                               // 1485239715515(ms)
    public static final String FIELD_CLFILE_INNER_CREATE_MONTH = "create_month"; // string
                                                                                 // 201701
    public static final String FIELD_CLFILE_INNER_UPDATE_USER = "update_user"; // string,
                                                                               // user
    public static final String FIELD_CLFILE_INNER_UPDATE_TIME = "update_time"; // long,
                                                                               // 1485239715515(ms)
    // CL_FILE & CL_FILE_HISTORY FILE file attribute(file)
    public static final String FIELD_CLFILE_FILE_CLASS_ID = "class_id"; // String
    public static final String FIELD_CLFILE_FILE_DATA_CREATE_TIME = "data_create_time"; // long,
                                                                                        // 1485239715515(ms)
    public static final String FIELD_CLFILE_FILE_DATA_ID = "data_id"; // string,
                                                                      // data_id
    public static final String FIELD_CLFILE_FILE_DATA_TYPE = "data_type"; // int,
                                                                          // 1
                                                                          // lob;
                                                                          // 2
                                                                          // bson
    public static final String FIELD_CLFILE_FILE_SIZE = "size"; // long, 1
    public static final String FIELD_CLFILE_FILE_SITE_LIST = "site_list"; // array,
                                                                          // [{site_id:1,
                                                                          // last_access_time:1485239715515}]
                                                                          // ws_version
    public static final String FIELD_CLFILE_FILE_SITE_LIST_ID = "site_id"; // int
    // last read content's time
    public static final String FIELD_CLFILE_FILE_SITE_LIST_TIME = "last_access_time";// long
    // last create content's time
    public static final String FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME = "create_time";// long
    // the workspace version when writing the site
    public static final String FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION = "ws_version";// int

    public static final String FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME = "table_name";// string

    public static final String FIELD_CLFILE_FILE_TITLE = "title"; // string,
                                                                  // mytitle1
    public static final String FIELD_CLFILE_FILE_AUTHOR = "author"; // string,
                                                                    // author1
    public static final String FIELD_CLFILE_FILE_MIME_TYPE = "mime_type"; // string,

    // md5 的 base64 编码字符串
    public static final String FIELD_CLFILE_FILE_MD5 = "md5";
    // 存储 md5 的 hex 编码字符串，增加这个字段主要给 s3 使用，避免需要转换 md5
    public static final String FIELD_CLFILE_FILE_ETAG = "etag";
    public static final String FIELD_CLFILE_FILE_BUCKET_ID = "bucket_id";

    // 不要在 external_data 内使用嵌套字段，ScmFileOperateUtils.formatFileObj
    // 拷贝这个字段的值时，没有考虑嵌套字段的情况
    public static final String FIELD_CLFILE_FILE_EXTERNAL_DATA = "external_data";

    public static final String FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH = "file_name_before_attach";
    public static final String FIELD_CLFILE_CUSTOM_METADATA = "custom_metadata";
    public static final String FIELD_CLFILE_VERSION_SERIAL = "version_serial";
    public static final String FIELD_CLFILE_DELETE_MARKER = "delete_marker";
    public static final String FIELD_CLFILE_CUSTOM_TAG = "custom_tag";

    // CL_FILE only(file & document)
    public static final String FIELD_CLFILE_EXTRA_STATUS = "status"; // int, 0
                                                                     // normal;
                                                                     // 1
                                                                     // creating;
                                                                     // 2
                                                                     // deleting;
    public static final String FIELD_CLFILE_EXTRA_TRANS_ID = "transaction_id"; // string,
                                                                               // transid

    public static final String FIELD_CLFILE_ACCESS_HISTORY = "access_history";
    public static final String FIELD_CLFILE_ACCESS_HISTORY_ID = "site_id";
    public static final String FIELD_CLFILE_ACCESS_HISTORY_LAST_ACCESS_TIME_HIS = "last_access_time_his";
    // CL_FILE_HISTORY only(file & document)
    public static final String FIELD_CLFILEHISTORY_FLAG = "history_flag"; // int
    public static final String FIELD_CLFILEHISTORY_USER = "history_user"; // string
                                                                          // user
    public static final String FIELD_CLFILEHISTORY_CREATETIME = "history_create_time";// time,
                                                                                      // 1485239715515(ms)

    // CL_TRANS_LOG
    public static final String FIELD_CLTRANS_ID = "id"; // string, xadfd
    public static final String FIELD_CLTRANS_TYPE = "type"; // int, 1 delete
                                                            // file;
    public static final String FIELD_CLTRANS_INDEX = "index"; // int,
    public static final String FIELD_CLTRANS_CREATETIME = "create_time"; // time,
                                                                         // 1485239715515(ms)
    public static final String FIELD_CLTRANS_USER = "user"; // string, user
    public static final String FIELD_CLTRANS_FILEID = "file_id"; // string,
                                                                 // 123xx
    public static final String FIELD_CLTRANS_MAJORVERSION = FIELD_CLFILE_MAJOR_VERSION;
    public static final String FIELD_CLTRANS_MINORVERSION = FIELD_CLFILE_MINOR_VERSION;

    public static final String FIELD_CLREL_FILEID = "file_id";
    public static final String FIELD_CLREL_FILENAME = "file_name";
    public static final String FIELD_CLREL_DIRECTORY_ID = FIELD_CLFILE_DIRECTORY_ID;
    public static final String FIELD_CLREL_CREATE_TIME = FIELD_CLFILE_INNER_CREATE_TIME;
    public static final String FIELD_CLREL_USER = FIELD_CLFILE_INNER_USER;
    public static final String FIELD_CLREL_UPDATE_TIME = FIELD_CLFILE_INNER_UPDATE_TIME;
    public static final String FIELD_CLREL_UPDATE_USER = FIELD_CLFILE_INNER_UPDATE_USER;
    public static final String FIELD_CLREL_FILE_SIZE = FIELD_CLFILE_FILE_SIZE;
    public static final String FIELD_CLREL_MAJOR_VERSION = FIELD_CLFILE_MAJOR_VERSION;
    public static final String FIELD_CLREL_MINOR_VERSION = FIELD_CLFILE_MINOR_VERSION;
    public static final String FIELD_CLREL_FILE_MIME_TYPE = FIELD_CLFILE_FILE_MIME_TYPE;
    public static final String FIELD_CLREL_FILE_DELETE_MARKER = FIELD_CLFILE_DELETE_MARKER;

    public static final String FIELD_CLDIR_ID = "id";
    public static final String FIELD_CLDIR_NAME = "name";
    public static final String FIELD_CLDIR_USER = "user";
    public static final String FIELD_CLDIR_CREATE_TIME = "create_time";
    public static final String FIELD_CLDIR_UPDATE_TIME = "update_time";
    public static final String FIELD_CLDIR_UPDATE_USER = "update_user";
    public static final String FIELD_CLDIR_PARENT_DIRECTORY_ID = "parent_directory_id";
    public static final String FIELD_CLDIR_VERSION = "version";

    public static final String FIELD_CLBREAKPOINTFILE_FILE_NAME = "file_name";

    // CL DATA_TABLE_NAME_HOISTORY
    public static final String FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_NAME = "workspace_name";
    public static final String FIELD_CLTABLE_NAME_HISTORY_SITE_NAME = "site_name";
    public static final String FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_IS_DELTED = "workspace_is_deleted";
    public static final String FIELD_CLTABLE_NAME_HISTORY_TABLE_CREATE_TIME = "table_create_time";
    public static final String FIELD_CLTABLE_NAME_HISTORY_TABLE_NAME = "table_name";

    // CL_SUBSCRIBER
    public static final String FIELD_CLSUBSCRIBER_CONFIG_NAME = "config_name";
    public static final String FIELD_CLSUBSCRIBER_SERVICE_NAME = "service_name";

    // CL_CONTENTSERVER_CONF_VERSION
    public static final String FIELD_CLVERSION_BUSINESS_TYPE = "business_type";
    public static final String FIELD_CLVERSION_BUSINESS_NAME = "business_name";
    public static final String FIELD_CLVERSION_BUSINESS_VERSION = "business_version";

    // CL_SPACE_RECYCLING_LOG
    public static final String FIELD_CLRECYCLE_SITE_ID = "site_id";
    public static final String FIELD_CLRECYCLE_DATA_SOURCE_TYPE = "data_source_type";
    public static final String FIELD_CLRECYCLE_LOG_INFO = "log_info";
    public static final String FIELD_CLRECYCLE_TIME = "time";

    // CL_DATA_TABLE_NAME_ACTIVE
    public static final String FIELD_CLACTIVE_WORKSPACE_NAME = "workspace_name";
    public static final String FIELD_CLACTIVE_RULE_TABLE_NAME = "rule_table_name";
    public static final String FIELD_CLACTIVE_ACTIVE_TABLE_NAME = "active_table_name";
    public static final String FIELD_CLACTIVE_SITE_ID = "site_id";

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

        public static final String FIELD_START_EXECUTE_TIME = "start_execute_time";
        public static final String FIELD_SUCCESS_COUNT = "success_count";
        public static final String FIELD_FAIL_COUNT = "fail_count";
        public static final String FIELD_SCHEDULE_ID = "schedule_id";
        public static final String FIELD_TARGET_SITE = "target_site";
        public static final String FIELD_SCOPE = "scope";
        public static final String FIELD_MAX_EXEC_TIME = "max_exec_time";
        public static final String FIELD_OPTION = "option";
        public static final String FIELD_EXTRA_INFO = "extra_info";
        public static final String FIELD_OPTION_QUICK_START = "quick_start";
        public static final String FIELD_OPTION_DATA_CHECK_LEVEL = "data_check_level";
        public static final String FIELD_OPTION_IS_RECYCLE_SPACE = "is_recycle_space";
        public static final String FIELD_OPTION_RECYCLE_SCOPE = "recycle_scope";
    }

    public static class ReloadBizConf {
        public static final String FIELD_SERVER_ID = "server_id";
        public static final String FIELD_SITE_ID = "site_id";
        public static final String FIELD_HOSTNAME = "hostname";
        public static final String FIELD_PORT = "port";
        public static final String FIELD_FLAG = "flag";
        public static final String FIELD_ERRORMSG = "errormsg";
    }

    public static class Batch {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_CLASS_ID = "class_id";
        public static final String FIELD_CLASS_PROPERTIES = "class_properties"; // array,
                                                                                // [{key:value}]
        public static final String FIELD_TAGS = "tags"; // array, [{key:value}]
        public static final String FIELD_FILES = "files"; // array, [{id:""}]
        // inner attribute
        public static final String FIELD_INNER_CREATE_USER = "create_user"; // string,
                                                                            // user
        public static final String FIELD_INNER_CREATE_TIME = "create_time"; // long,
                                                                            // 1485239715515(ms)
        public static final String FIELD_INNER_UPDATE_USER = "update_user"; // string,
                                                                            // user
        public static final String FIELD_INNER_UPDATE_TIME = "update_time"; // long,
                                                                            // 1485239715515(ms)
        public static final String FIELD_INNER_CREATE_MONTH = "create_month";

    }

    public static class BreakpointFile {
        private BreakpointFile() {
        }

        public static final String FIELD_FILE_NAME = "file_name";
        public static final String FIELD_SITE_NAME = "site_name";
        public static final String FIELD_CHECKSUM_TYPE = "checksum_type";
        public static final String FIELD_CHECKSUM = "checksum";
        public static final String FIELD_DATA_ID = "data_id";
        public static final String FIELD_COMPLETED = "completed";
        public static final String FIELD_UPLOAD_SIZE = "upload_size";
        public static final String FIELD_CREATE_USER = "create_user";
        public static final String FIELD_CREATE_TIME = "create_time";
        public static final String FIELD_UPLOAD_USER = "upload_user";
        public static final String FIELD_UPLOAD_TIME = "upload_time";
        public static final String FIELD_IS_NEED_MD5 = "is_need_md5";
        public static final String FIELD_MD5 = "md5";
    }

    public static class Role {
        private Role() {
        }

        public static final String FIELD_ROLE_ID = "role_id";
        public static final String FIELD_ROLE_NAME = "role_name";
        public static final String FIELD_DESCRIPTION = "description";
    }

    public static class Privilege {
        private Privilege() {
        }

        public static final String FIELD_META_VERSION = "version";

        public static final String FIELD_PRIVILEGE_ID = "id";
        public static final String FIELD_PRIVILEGE_ROLE_TYPE = "role_type";
        public static final String FIELD_PRIVILEGE_ROLE_ID = "role_id";
        public static final String FIELD_PRIVILEGE_RESOURCE_ID = "resource_id";
        public static final String FIELD_PRIVILEGE_PRIVILEGE = "privilege";
    }

    public static class Resource {
        private Resource() {
        }

        public static final String FIELD_RESOURCE_ID = "id";
        public static final String FIELD_RESOURCE_TYPE = "type";
        public static final String FIELD_RESOURCE = "resource";
    }

    public static class User {
        private User() {
        }

        public static final String FIELD_USER_ID = "user_id";
        public static final String FIELD_USERNAME = "username";
        public static final String FIELD_PASSWORD_TYPE = "password_type";
        public static final String FIELD_ROLES = "roles";
        public static final String FIELD_ENABLED = "enabled";
    }

    public static class SessionInfo {
        private SessionInfo() {
        }

        public static final String FIELD_SESSION_ID = "session_id";
        public static final String FIELD_USERNAME = "username";
        public static final String FIELD_CREATION_TIME = "creation_time";
        public static final String FIELD_LAST_ACCESSED_TIME = "last_accessed_time";
        public static final String FIELD_MAX_INACTIVE_INTERVAL = "max_inactive_interval";
    }

    public static final class Strategy {
        private Strategy() {
        }

        public static final String FIELD_SOURCE_SITE = "source_site";
        public static final String FIELD_TARGET_SITE = "target_site";
        public static final String FIELD_CONNECTIVITY = "connectivity";
    }

    public static final class Class {
        private Class() {
        }

        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_DESCRIPTION = "description";
        public static final String FIELD_INNER_CREATE_USER = "create_user";
        public static final String FIELD_INNER_CREATE_TIME = "create_time";
        public static final String FIELD_INNER_UPDATE_USER = "update_user";
        public static final String FIELD_INNER_UPDATE_TIME = "update_time";

        public static final String REL_ATTR_INFOS = "attrs";
    }

    public static final class Attribute {
        private Attribute() {
        }

        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_DISPLAY_NAME = "display_name";
        public static final String FIELD_DESCRIPTION = "description";
        public static final String FIELD_TYPE = "type";
        public static final String FIELD_CHECK_RULE = "check_rule";
        public static final String FIELD_REQUIRED = "required";
        public static final String FIELD_INNER_CREATE_USER = "create_user";
        public static final String FIELD_INNER_CREATE_TIME = "create_time";
        public static final String FIELD_INNER_UPDATE_USER = "update_user";
        public static final String FIELD_INNER_UPDATE_TIME = "update_time";
    }

    public static final class ClassAttrRel {
        private ClassAttrRel() {
        }

        public static final String FIELD_CLASS_ID = "class_id";
        public static final String FIELD_ATTR_ID = "attr_id";
    }

    public static class Audit {
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String TYPE = "type";
        public static final String USER_TYPE = "user_type";
        public static final String USER_NAME = "user_name";
        public static final String WORK_SPACE = "work_space";
        public static final String FLAG = "flag";
        public static final String TIME = "time";
        public static final String THREAD = "thread";
        public static final String LEVEL = "level";
        public static final String MESSAGE = "message";
    }

    public static class DataTableNameHistory {
        public static final String WORKSPACE_NAME = "workspace_name";
        public static final String SITE_NAME = "site_name";
        public static final String WORKSPACE_IS_DELETED = "workspace_is_deleted";
        public static final String TABLE_CREATE_TIME = "table_create_time";
        public static final String TABLE_NAME = "table_name";
    }

    public static final class Traffic {
        private Traffic() {
        }

        public static final String FIELD_TYPE = "type";
        public static final String FIELD_WORKSPACE_NAME = "workspace_name";
        public static final String FIELD_TRAFFIC = "traffic";
        public static final String FIELD_RECORD_TIME = "record_time";
    }

    public static final class FileDelta {
        private FileDelta() {
        }

        public static final String FIELD_WORKSPACE_NAME = "workspace_name";
        public static final String FIELD_COUNT_DELTA = "count_delta";
        public static final String FIELD_SIZE_DELTA = "size_delta";
        public static final String FIELD_RECORD_TIME = "record_time";
    }

    public static final class ObjectDelta {
        private ObjectDelta() {
        }

        public static final String FIELD_BUCKET_NAME = "bucket_name";
        public static final String FIELD_COUNT_DELTA = "count_delta";
        public static final String FIELD_SIZE_DELTA = "size_delta";
        public static final String FIELD_RECORD_TIME = "record_time";
        public static final String FIELD_UPDATE_TIME = "update_time";

    }

    public static final class ContentLocation {
        private ContentLocation(){}
        public static final String FIELD_TYPE = "type";
        public static final String FIELD_SITE = "site";
        public static final String FIELD_CL = "cl";
        public static final String FIELD_CS = "cs";
        public static final String FIELD_LOB_ID = "lob_id";
        public static final String FIELD_URLS = "urls";
        public static final String FIELD_OBJECT_ID = "object_id";
        public static final String FIELD_BUCKET = "bucket";
        public static final String FIELD_CONTAINER = "container";
        public static final String FIELD_TABLE_NAME = "table_name";
        public static final String FIELD_DIRECTORY = "directory";
        public static final String FIELD_FILE_NAME = "file_name";
        public static final String FIELD_FILE_PATH = "file_path";
    }

    public static final class Bucket {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String FILE_TABLE = "file_table";
        public static final String CREATE_USER = "create_user";
        public static final String CREATE_TIME = "create_time";
        public static final String WORKSPACE = "workspace";
        public static final String VERSION_STATUS = "version_control_status";
        public static final String CUSTOM_TAG = "custom_tag";
        public static final String UPDATE_TIME = "update_time";
        public static final String UPDATE_USER = "update_user";
    }

    public static final class BucketFile {
        public static final String FILE_ID = FieldName.FIELD_CLFILE_ID;
        public static final String FILE_NAME = FieldName.FIELD_CLFILE_NAME;
        public static final String FILE_ETAG = FieldName.FIELD_CLFILE_FILE_ETAG;
        public static final String FILE_MAJOR_VERSION = FieldName.FIELD_CLFILE_MAJOR_VERSION;
        public static final String FILE_MINOR_VERSION = FieldName.FIELD_CLFILE_MINOR_VERSION;
        public static final String FILE_UPDATE_TIME = FieldName.FIELD_CLFILE_INNER_UPDATE_TIME;
        public static final String FILE_CREATE_USER = FieldName.FIELD_CLFILE_INNER_USER;
        public static final String FILE_MIME_TYPE = FieldName.FIELD_CLFILE_FILE_MIME_TYPE;
        public static final String FILE_SIZE = FieldName.FIELD_CLFILE_FILE_SIZE;
        public static final String FILE_CREATE_TIME = FieldName.FIELD_CLFILE_INNER_CREATE_TIME;
        public static final String FILE_VERSION_SERIAL = FieldName.FIELD_CLFILE_VERSION_SERIAL;
        public static final String FILE_DELETE_MARKER = FieldName.FIELD_CLFILE_DELETE_MARKER;
    }

    public static final class TraceSpan {
        public static final String TRACE_ID = "traceId";
        public static final String SPAN_ID = "id";
        public static final String PARENT_SPAN_ID = "parentId";
        public static final String SPAN_NAME = "name";
        public static final String TIMESTAMP = "timestamp";
        public static final String DURATION = "duration";
        public static final String ANNOTATIONS = "annotations";
        public static final String TAGS = "binaryAnnotations";
    }

    public static final class TraceAnnotation {
        public static final String TIMESTAMP = "timestamp";
        public static final String VALUE = "value";
        public static final String ENDPOINT = "endpoint";
        public static final String SERVICE_NAME = "serviceName";
        public static final String IP = "ipv4";
        public static final String PORT = "port";
    }

    public static final class ScmFileLocation {
        public static final String SITE_ID = "siteId";
        public static final String DATE = "date";
        public static final String CREATE_DATE = "createDate";
        public static final String WS_VERSION = "wsVersion";
        public static final String TABLE_NAME = "tableName";
    }

    public static final class LifeCycleConfig {
        public static final String FIELD_STAGE_TAG_CONFIG = "stage_tag";
        public static final String FIELD_TRANSITION_CONFIG = "transition";
        public static final String FIELD_STAGE_TAG_NAME = "name";
        public static final String FIELD_STAGE_TAG_DESC = "desc";
        public static final String FIELD_TRANSITION_FLOW = "flow";
        public static final String FIELD_TRANSITION_FLOW_SOURCE = "source";
        public static final String FIELD_TRANSITION_FLOW_DEST = "dest";

        public static final String FIELD_TRANSITION_NAME = "name";
        public static final String FIELD_TRANSITION_MATCHER = "matcher";
        public static final String FIELD_TRANSITION_TRANSITION_TRIGGERS = "transition_triggers";
        public static final String FIELD_TRANSITION_TRIGGERS_MODE = "mode";
        public static final String FIELD_TRANSITION_TRIGGERS_MAX_EXEC_TIME = "max_exec_time";
        public static final String FIELD_TRANSITION_TRIGGERS_RULE = "rule";
        public static final String FIELD_TRANSITION_TRIGGERS_TRIGGER_LIST = "trigger_list";
        public static final String FIELD_TRANSITION_TRIGGER_ID = "id";
        public static final String FIELD_TRANSITION_TRIGGER_MODE = "mode";
        public static final String FIELD_TRANSITION_TRIGGER_CREATE_TIME = "create_time";
        public static final String FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME = "last_access_time";
        public static final String FIELD_TRANSITION_TRIGGER_BUILD_TIME = "build_time";
        public static final String FIELD_TRANSITION_TRIGGER_TRANSITION_TIME = "transition_time";
        public static final String FIELD_TRANSITION_EXTRA_CONTENT = "extra_content";
        public static final String FIELD_EXTRA_CONTENT_DATA_CHECK_LEVEL = "data_check_level";
        public static final String FIELD_EXTRA_CONTENT_QUICK_START = "quick_start";
        public static final String FIELD_EXTRA_CONTENT_RECYCLE_SPACE = "recycle_space";
        public static final String FIELD_EXTRA_CONTENT_SCOPE = "scope";
        public static final String FIELD_TRANSITION_CLEAN_TRIGGERS = "clean_triggers";
        public static final String FIELD_CLEAN_TRIGGERS_MODE = "mode";
        public static final String FIELD_CLEAN_TRIGGERS_MAX_EXEC_TIME = "max_exec_time";
        public static final String FIELD_CLEAN_TRIGGERS_RULE = "rule";
        public static final String FIELD_CLEAN_TRIGGERS_TRIGGER_LIST = "trigger_list";
        public static final String FIELD_TRANSITION = "transition";
        public static final String FIELD_TRANSITION_SCHEDULE_ID = "id";
        public static final String FIELD_TRANSITION_SCHEDULE_WORKSPACE = "workspace";
        public static final String FIELD_TRANSITION_SCHEDULE_CREATE_USER = "create_user";
        public static final String FIELD_TRANSITION_SCHEDULE_UPDATE_USER = "update_user";
        public static final String FIELD_TRANSITION_SCHEDULE_CREATE_TIME = "create_time";
        public static final String FIELD_TRANSITION_SCHEDULE_UPDATE_TIME = "update_time";
        public static final String FIELD_TRANSITION_SCHEDULE_ENABLE = "enable";
        public static final String FIELD_TRANSITION_SCHEDULE_CUSTOMIZED = "customized";
        public static final String FIELD_TRANSITION_SCHEDULE_PREFERRED_REGION = "preferred_region";
        public static final String FIELD_TRANSITION_SCHEDULE_PREFERRED_ZONE = "preferred_zone";
        public static final String FIELD_TRANSITION_SCHEDULE_TRANSITION = "transition";
        public static final String FIELD_TRANSITION_SCHEDULE_IDS = "schedule_ids";
        public static final String FIELD_MAX_STAY_TIME = "max_stay_time";
        public static final String FIELD_LIFE_CYCLE_CONFIG = "life_cycle_config";

    }

    public static class GlobalConfig {
        public static final String FIELD_CONFIG_NAME = "config_name";
        public static final String FIELD_CONFIG_VALUE = "config_value";
    }

    public static class TagLib {
        public static final String TAG_ID = "tag_id";
        public static final String TAG_TYPE = "tag_type";

        public static final String TAG = "tag";
        public static final String CUSTOM_TAG = "custom_tag";
        public static final String CUSTOM_TAG_TAG_KEY = "key";
        public static final String CUSTOM_TAG_TAG_VALUE = "value";

        public static BSONObject tagLibFulltextIdxDef() {
            BSONObject indexDef = new BasicBSONObject();
            indexDef.put(FieldName.TagLib.TAG_ID, "text");
            indexDef.put(FieldName.TagLib.TAG_TYPE, "text");
            indexDef.put(FieldName.TagLib.TAG, "text");
            indexDef.put(FieldName.TagLib.CUSTOM_TAG, "text");
            return indexDef;
        }

        public static BSONObject tagLibFulltextIdxAttr() {
            BSONObject idxAttr = new BasicBSONObject();
            BSONObject mappings = new BasicBSONObject();
            BSONObject fields = new BasicBSONObject();
            fields.put(FieldName.TagLib.TAG_ID, new BasicBSONObject("Index", false));
            fields.put(FieldName.TagLib.TAG_TYPE, new BasicBSONObject("Index", false));
            fields.put(FieldName.TagLib.TAG, new BasicBSONObject("Type", "keyword"));
            fields.put(FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                    new BasicBSONObject("Type", "keyword"));
            fields.put(FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                    new BasicBSONObject("Type", "keyword"));
            mappings.put("Fields", fields);
            idxAttr.put("Mappings", mappings);
            return idxAttr;
        }
    }

    public static final class Quota {
        public static final String TYPE = "type";
        public static final String NAME = "name";
        public static final String ENABLE = "enable";
        public static final String MAX_SIZE = "max_size";
        public static final String MAX_OBJECTS = "max_objects";
        public static final String USED_SIZE = "used_size";
        public static final String USED_OBJECTS = "used_objects";
        public static final String UPDATE_TIME = "update_time";
        public static final String QUOTA_ROUND_NUMBER = "quota_round_number";
        public static final String EXTRA_INFO = "extra_info";
        public static final String EXTRA_INFO_WORKSPACE = "workspace";
        public static final String VERSION = "version";
    }

    public static final class QuotaSync {
        public static final String TYPE = "type";
        public static final String NAME = "name";
        public static final String STATISTICS_SIZE = "statistics_size";
        public static final String STATISTICS_OBJECTS = "statistics_objects";
        public static final String STATUS = "status";
        public static final String ERROR_MSG = "error_msg";
        public static final String EXPIRE_TIME = "expire_time";
        public static final String SYNC_ROUND_NUMBER = "sync_round_number";
        public static final String QUOTA_ROUND_NUMBER = "quota_round_number";
        public static final String IS_FIRST_SYNC = "is_first_sync";
        public static final String BEGIN_TIME = "begin_time";
        public static final String END_TIME = "end_time";
        public static final String AGREEMENT_TIME = "agreement_time";
        public static final String STATISTICS_DETAIL = "statistics_detail";
        public static final String EXTRA_INFO = "extra_info";
        public static final String EXTRA_INFO_WORKSPACE = "workspace";
        public static final String UPDATE_TIME = "update_time";
    }

    public static final class QuotaStatisticsProgress {
        public static final String ESTIMATED_TIME = "estimated_time";
        public static final String PROCESS_NODE = "process_node";
        public static final String PROCESS_TIME = "process_time";
        public static final String UPDATE_TIME = "update_time";
        public static final String STATUS = "status";
        public static final String ERROR_MSG = "error_msg";
    }

}