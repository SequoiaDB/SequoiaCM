package com.sequoiacm.common;

import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.net.ConfigOptions;

/**
 * Provides classes defining of scm type-safe constant values.
 */
public class CommonDefine {
    private CommonDefine() {
    }

    public static class DefaultValue {
        private DefaultValue() {
            super();
        }

        // *********************** ROOT SITE ******************************
        public static final String ROOT_SITE_URL = "";
        public static final String ROOT_SITE_USER = "";
        public static final String ROOT_SITE_PASSWORD = "";
        public static final int ROOT_SITE_PASSWORD_TYPE = 0;

        // *********************** SEQUOIADB ******************************
        private static final ConfigOptions connConf = new ConfigOptions();
        public static final int SDB_CONNECT_TIMEOUT = connConf.getConnectTimeout();
        public static final int SDB_SOCKET_TIMEOUT = connConf.getSocketTimeout();
        public static final long SDB_MAX_CONN_RETRY_TIME = connConf.getMaxAutoConnectRetryTime();
        public static final boolean SDB_USE_NAGLE = connConf.getUseNagle();
        public static final boolean SDB_USE_SSL = connConf.getUseSSL();

        private static final DatasourceOptions dsConf = new DatasourceOptions();
        public static final int SDB_KEEP_ALIVE_TIME = 60 * 1000;
        public static final int SDB_MAX_CONN_NUM = dsConf.getMaxCount();
        public static final boolean SDB_VALIDATE_CONN = true;
        public static final int SDB_DELTA_INC_COUNT = dsConf.getDeltaIncCount();
        public static final int SDB_MAX_IDLE_NUM = dsConf.getMaxIdleCount();
        public static final int SDB_RECHECK_CYCL_PERIOD = 30 * 1000;

        // *********************** SERVER ******************************
        public static final int PORT = 0;

        // *********************** zoo keeper ******************************
        public static final String ZK_URL = "";
        public static final int ZK_LOCK_TIMEOUT = 120 * 1000; // (ms)
        public static final int ZK_CLIENT_TIMEOUT = 120 * 1000; // (ms)
        public static final long ZK_CLEANJOB_PERIOD = 120L * 1000L; // (ms)
        public static final long ZK_CLEANJOB_RESIDUAL = 180L * 1000L; // (ms)
        public static final int ZK_CLEANJOB_CHILDNUM_THRESHOLD = 1000;
        // for every 720 cleanups, clean up all zookeeper nodes
        public static final int ZK_CLEANJOB_COUNT_THRESHOLD = 12 * 60;

        // *********************** jvm ******************************
        public static final String JVM_OPTIONS = "-Xmx1024M -Xms1024M -Xmn256M";

        // ***********************task******************************
        public static final int TRANSFER_PERIOD_CHECK_LENGTH = 1024 * 1024 * 10;
    }

    public static class Scope {
        private Scope() {
            super();
        }

        public static final int SCOPE_CURRENT = 1;
        public static final int SCOPE_HISTORY = 2;
        public static final int SCOPE_ALL = 3;
    }

    /**
     * Provides a set of constants that to represent the type of seek.
     *
     * @since 2.1
     */
    public static class SeekType {
        private SeekType() {
            super();
        }

        /**
         * A constants representing that measured in bytes from the beginning.
         *
         * @since 2.1
         */
        public static final int SCM_FILE_SEEK_SET = 0;

        /**
         * A constants representing that measured in bytes from current position.
         *
         * @since 2.1
         */
        @Deprecated
        public static final int SCM_FILE_SEEK_CUR = 1;

        /**
         * A constants representing that measured in bytes from current end.
         *
         * @since 2.1
         */
        @Deprecated
        public static final int SCM_FILE_SEEK_END = 2;
    }

    public static class ReadFileFlag {

        private ReadFileFlag() {
            super();
        }

        // if return data wit response
        public static final int SCM_READ_FILE_WITHDATA = 0x00000001;

        // if support seek
        public static final int SCM_READ_FILE_NEEDSEEK = 0x00000002;

        // if read local site only
        public static final int SCM_READ_FILE_LOCALSITE = 0x00000004;

        // do not cache local when reading across sites
        public static final int SCM_READ_FILE_FORCE_NO_CACHE = 0x00000008;
    }

    public static class NodeScope {

        private NodeScope() {
            super();

        }

        // all nodes in all centers
        public static final int SCM_NODESCOPE_ALL = 1;
        // all nodes in the same center
        public static final int SCM_NODESCOPE_CENTER = 2;
        // only this node itself
        public static final int SCM_NODESCOPE_NODE = 3;
    }

    /**
     * Provides a set of constants that to represent the type of task.
     *
     * @since 2.1
     */
    public static class TaskType {
        private TaskType() {
            super();
        }

        /**
         * A constants representing the transfer task of file.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_TRANSFER_FILE = 1;

        /**
         * A constants representing the clean task of file.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_CLEAN_FILE = 2;
    }

    /**
     * Provides a set of constants that to represent the running flag of task.
     *
     * @since 2.1
     */
    public static class TaskRunningFlag {

        private TaskRunningFlag() {
            super();
        }

        /**
         * A constants representing the task is init.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_INIT = 1;

        /**
         * A constants representing the task is running.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_RUNNING = 2;
        /**
         * A constants representing the task is finish.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_FINISH = 3;
        /**
         * A constants representing the task is cancel.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_CANCEL = 4;
        /**
         * A constants representing the task is abort.
         *
         * @since 2.1
         */
        public static final int SCM_TASK_ABORT = 5;

        /**
         * A constants representing the task is interrupted because of timeout.
         *
         * @since 3.0
         */
        public static final int SCM_TASK_TIMEOUT = 6;
    }

    public static class TaskNotifyType {
        private TaskNotifyType() {
            super();

        }

        public static final int SCM_TASK_CREATE = 1;
        public static final int SCM_TASK_STOP = 2;
    }

    public static class CryptType {
        private CryptType() {
            super();
        }

        public static final int SCM_CRYPT_TYPE_NONE = 0;
        public static final int SCM_CRYPT_TYPE_DES = 1;
    }

    public static class DataSourceType {
        private DataSourceType() {
            super();
        }

        public static final int SCM_DATASOURCE_TYPE_SEQUOIADB = 1;
        public static final int SCM_DATASOURCE_TYPE_HBASE = 2;
        public static final int SCM_DATASOURCE_TYPE_CEPHS3 = 3;
        public static final int SCM_DATASOURCE_TYPE_CEPHSWIFT = 4;
        public static final int SCM_DATASOURCE_TYPE_HDFS = 5;

        public static final String SCM_DATASOURCE_TYPE_SEQUOIADB_STR = "sequoiadb";
        public static final String SCM_DATASOURCE_TYPE_HBASE_STR = "hbase";
        public static final String SCM_DATASOURCE_TYPE_CEPHS3_STR = "ceph_s3";
        public static final String SCM_DATASOURCE_TYPE_CEPHSWIFT_STR = "ceph_swift";
        public static final String SCM_DATASOURCE_TYPE_HDFS_STR = "hdfs";
    }

    /**
     * Provides a set of constants that to represent the status of process.
     */
    public static class ScmProcessStatus {
        private ScmProcessStatus() {
            super();
        }

        /**
         * A constants representing the process is starting.
         *
         * @since 2.1
         */
        public static final String SCM_PROCESS_STATUS_STARTING = "starting";
        /**
         * A constants representing the process is running.
         *
         * @since 2.1
         */
        public static final String SCM_PROCESS_STATUS_RUNING = "running";
    }

    public static class RestArg {
        public static final String DATA_LENGTH = "data_length";

        public static final String WORKSPACE_NAME = "workspace_name";
        public static final String GET_WORKSPACE_REPS = "workspace";
        public static final String WORKSPACE_CONF = "workspace_conf";
        public static final String WORKSPACE_LOCATION_SITE_NAME = "site";
        public static final String WORKSPACE_LOCATION_TYPE = "location_type";
        public static final String WORKSPACE_ENFORCED_DELETE = "enforced";
        public static final String WORKSPACE_UPDATOR = "updator";
        public static final String WORKSPACE_UPDATOR_ADD_DATA_LOCATION = "add_data_location";
        public static final String WORKSPACE_UPDATOR_REMOVE_DATA_LOCATION = "rmove_data_location";
        public static final String WORKSPACE_UPDATOR_DESCRIPTION = "description";
        public static final String WORKSPACE_FILTER = "filter";
        public static final String WORKSPACE_ORDERBY = "orderby";
        public static final String WORKSPACE_SKIP = "skip";
        public static final String WORKSPACE_LIMIT = "limit";

        public static final String DATASOURCE_DATA_ID = "data_id";
        public static final String DATASOURCE_DATA_TYPE = "data_type";
        public static final String DATASOURCE_DATA_CREATE_TIME = "create_time";
        public static final String DATASOURCE_DATA_HEADER = "data";
        public static final String DATASOURCE_DATA_SIZE = "size";
        public static final String DATASOURCE_DATA_TABLE_NAMES = "table_names";
        public static final String DATASOURCE_SITE_NAME = "site_name";

        public static final String FILE_ID = "id";
        public static final String FILE_READ_FLAG = "read_flag";
        public static final String FILE_MAJOR_VERSION = "major_version";
        public static final String FILE_MINOR_VERSION = "minor_version";
        public static final String FILE_IS_PHYSICAL = "is_physical";
        public static final String FILE_LIST_SCOPE = "scope";
        public static final String FILE_FILTER = "filter";
        public static final String FILE_DESCRIPTION = "description";
        public static final String FILE_INFO = "file_info";
        public static final String FILE_MULTIPART_FILE = "file";
        public static final String FILE_RESP_FILE_INFO = "file";
        public static final String FILE_BREAKPOINT_FILE = "breakpoint_file";
        public static final String FILE_READ_OFFSET = "offset";
        public static final String FILE_READ_LENGTH = "length";
        public static final String FILE_SKIP = "skip";
        public static final String FILE_LIMIT = "limit";
        public static final String FILE_ORDERBY = "orderby";
        public static final String FILE_SELECTOR = "selector";
        public static final String FILE_ASYNC_TRANSFER_TARGET_SITE = "target_site";

        public static final String FILE_EXTERNAL_DATA = "externald_data";

        public static final String FILE_UPLOAD_CONFIG = "upload_config";
        public static final String FILE_IS_OVERWRITE = "is_overwrite";
        public static final String FILE_IS_NEED_MD5 = "is_need_md5";

        public static final String FILE_UPDATE_CONTENT_OPTION = "update_content_option";

        public static final String BIZ_RELOAD_SCOPE = "scope";
        public static final String BIZ_RELOAD_ID = "id";
        public static final String BIZ_RELOAD_METADATA_ONLY = "metadata_only";

        public static final String GET_PROP_KEYS = "keys";
        public static final String GET_PROP_KEY = "key";
        public static final String GET_PROP_RESP_CONF = "conf";

        public static final String TASK_INFO_RESP = "task";
        public static final String CREATE_TASK_TYPE = "task_type";
        public static final String CREATE_TASK_OPTIONS = "options";
        public static final String CREATE_TASK_SERVER_ID = "server_id";
        public static final String CREATE_TASK_TARGET_SITE = "target_site";
        public static final String TASK_ID = "id";
        public static final String TASK_FILTER = "filter";
        public static final String TASK_NOTIFY_TYPE = "notify_type";
        public static final String TASK_SCOPE = "scope";
        public static final String TASK_MAX_EXEC_TIME = "max_exec_time";

        public static final String BATCH_WS_NAME = "workspace_name";
        public static final String BATCH_DESCRIPTION = "description";
        public static final String BATCH_FILTER = "filter";
        public static final String BATCH_FILE_ID = "file_id";
        public static final String BATCH_OBJECT = "batch";
        public static final String BATCH_FILES_COUNT = "files_count";
        public static final String BATCH_ORDERBY = "orderby";
        public static final String BATCH_SKIP = "skip";
        public static final String BATCH_LIMIT = "limit";

        public static final String METADATA_DESCRIPTION = "description";
        public static final String METADATA_FILTER = "filter";
        public static final String METADATA_CLASSINFO_RESP = "class";
        public static final String METADATA_CLASS_NAME = "class_name";
        public static final String METADATA_ATTRINFO_RESP = "attr";

        public static final String AUDIT_WS_NAME = "workspace_name";
        public static final String AUDIT_FILTER = "filter";

        public static final String REFRESH_STATISTICS_WS_NAME = "workspace_name";
        public static final String REFRESH_STATISTICS_TYPE = "statistics_type";

        public static final String CONFIG = "config";
        public static final String IS_ASYNC_NOTIFY = "is_async_notify";

        public static final String ACTION_CALC_MD5 = "calc_md5";
        public static final String ACTION_GET_SITE_STRATEGY = "get_site_strategy";
        public static final String ACTION_GET_CONTENT_LOCATION = "get_content_location";

        public static final String SITE_STRATEGY = "strategy";

        public static final String X_SCM_COUNT = "X-SCM-Count";

        public static final String FILTER = "filter";
        public static final String ORDER_BY = "order_by";
        public static final String SKIP = "skip";
        public static final String LIMIT = "limit";
        public static final String BUCKET_NAME = "bucket_name";
        public static final String FILE_NAME = "file_name";
        public static final String FILE_ID_LIST = "file_id_list";
        public static final String ATTACH_KEY_TYPE = "key_type";
        public static final String ATTACH_FAILURE_FILE_ID = "file_id";
        public static final String ATTACH_FAILURE_ERROR_CODE = "error_code";
        public static final String ATTACH_FAILURE_ERROR_MSG = "error_msg";
        public static final String ATTACH_FAILURE_EXT_INFO = "external_info";
        public static final String ATTACH_FAILURE_EXT_INFO_BUCKET_NAME = "bucket_name";

    }

    public static class Directory {
        public static final String SCM_ROOT_DIR_ID = "000000000000000000000000";
        public static final String SCM_ROOT_DIR_NAME = "/";
        public static final String SCM_ROOT_DIR_PARENT_ID = "-1";
        public static final String SCM_DIR_SEP = SCM_ROOT_DIR_NAME;
        public static final char SCM_DIR_SEP_CHAR = '/';

        public static final String SCM_REST_ARG_NAME = "name";
        public static final String SCM_REST_ARG_WORKSPACE_NAME = "workspace_name";
        public static final String SCM_REST_ARG_DIRECTORY = "directory";

        public static final String SCM_REST_ARG_PARENT_DIR_PATH = "parent_directory_path";
        public static final String SCM_REST_ARG_PARENT_DIR_ID = "parent_directory_id";
        public static final String SCM_REST_ARG_PATH = "path";
        public static final String SCM_REST_ARG_ID = "id";

        public static final String SCM_REST_ARG_TYPE_ID = "id";
        public static final String SCM_REST_ARG_TYPE_PATH = "path";

        public static final String SCM_REST_ARG_CREATE_TIME = "create_time";

    }

    public static class File {
        public static final int UNTIL_END_OF_FILE = -1;
    }

    public static class Metrics {
        public static final String PREFIX_FILE_UPLOAD = "counter.file.upload.";
        public static final String PREFIX_FILE_DOWNLOAD = "counter.file.download.";
    }

    public static class StatisticsType {
        public static final int SCM_STATISTICS_TYPE_TRAFFIC = 1;
        public static final int SCM_STATISTICS_TYPE_FILE_DELTA = 2;
    }

    public static class TrafficType {
        public static final String SCM_TRAFFIC_TYPE_FILEUPLOAD = "file_upload";
        public static final String SCM_TRAFFIC_TYPE_FILEDOWNLOAD = "file_download";
    }

    public static class SiteStrategy {
        public static final String SITE_STRATEGY_STAR = "star";
        public static final String SITE_STRATEGY_NETWORK = "network";
    }
}
