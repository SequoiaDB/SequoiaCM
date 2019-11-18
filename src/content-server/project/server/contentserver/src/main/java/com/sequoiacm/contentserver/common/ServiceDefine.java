package com.sequoiacm.contentserver.common;

public class ServiceDefine {

    public static class FileHistoryFlag {
        public static final int NORMAL = 0;
        public static final int DELETED = 1;
    }

    public static class FileStatus {
        public static final int NORMAL = 0;
        public static final int MODIFYING = 1;
        public static final int DELETING = 2;
    }

    public static class CsName {
        public static final String CS_SCMSYSTEM = "SCMSYSTEM";
        public static final String CS_METADATA_EXTRA = "_META";
        public static final String CS_LOB_EXTRA = "_LOB";
    }

    public static class SystemClName {
        public static final String CL_SITE = "SITE";
        public static final String CL_CONTENTSERVER = "CONTENTSERVER";
        public static final String CL_WORKSPACE = "WORKSPACE";
        public static final String CL_USER = "USER";
        public static final String CL_SESSION = "SESSION";
        public static final String CL_TASK = "TASK";
    }

    public static class WorkspaceCLName {
        public static final String CL_FILE = "FILE";
        public static final String CL_FILE_HISTORY = "FILE_HISTORY";
        public static final String CL_TRANSACTION_LOG = "TRANSACTION_LOG";
        public static final String CL_LOB = "LOB";
    }

    public static class Session {
        public static final int SESSION_TYPE_THREADSESSION = 0;
        public static final int SESSION_TYPE_UNEXISTSESSION = 1;
        public static final int SESSION_TYPE_UNKNOWNOPSESSION = 2;
        // run message just once and exit thread
        public static final int SESSION_TYPE_THREADSESSION_ONCE = 3;
    }

    public static class TransType {
        public static final int DELETING_SINGLE_FILE = 0;
    }

    public static class Job {
        public static final int JOB_TYPE_TRANS_ROLLBACK = 0;
        public static final int JOB_TYPE_SESSION_KEEPALIVE = 1;
        public static final int JOB_TYPE_SESSION_CHECKALIVE = 2;
        public static final int JOB_TYPE_LOCK_CLEARNODES = 3;
        public static final int JOB_TYPE_TRANSFER_FILE = 4;
        public static final int JOB_TYPE_CACHE_FILE = 5;
        public static final int JOB_TYPE_LOG_RESOURCE = 6;
        public static final int JOB_TYPE_NET_POOL_CHECKSTAY = 7;
        public static final int JOB_TYPE_NET_POOL_KEEPALIVE = 8;

        public static final int JOB_TYPE_TASK = 100;
        public static final int JOB_TYPE_UPDATE_TASK_STATUS = 101;

        public static final int LOG_RESOURCE_JOB_PERIOD = 60 * 1000; // ms
        public static final int TRANS_ROLLBACK_TASK_DELAY = 2000; // ms
        public static final int TRANS_LOG_RESOURCE_DELAY = 2000; // ms
    }
}
