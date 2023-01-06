package com.sequoiacm.metasource;

public class MetaSourceDefine {

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
        public static final String CS_SCMAUDIT = "SCMAUDIT";
        public static final String CS_UDC = "UDC";
    }

    public static class SystemClName {
        public static final String CL_SITE = "SITE";
        public static final String CL_CONTENTSERVER = "CONTENTSERVER";
        public static final String CL_WORKSPACE = "WORKSPACE";
        public static final String CL_WORKSPACE_HISTORY = "WORKSPACE_HISTORY";
        public static final String CL_USER = "USER";
        public static final String CL_SESSION = "SESSION";
        public static final String CL_TASK = "TASK";
        public static final String CL_STRATEGY = "STRATEGY";
        public static final String CL_AUDIT = "AUDIT_LOG_EVENT";
        public static final String CL_DATA_TABLE_NAME_HISTORY = "DATA_TABLE_NAME_HISTORY";
        public static final String CL_SPACE_RECYCLING_LOG = "SPACE_RECYCLING_LOG";
        public static final String CL_DATA_BUCKET_NAME_ACTIVE = "DATA_BUCKET_NAME_ACTIVE";
    }

    public static class WorkspaceCLName {
        public static final String CL_FILE = "FILE";
        public static final String CL_FILE_HISTORY = "FILE_HISTORY";
        public static final String CL_BREAKPOINT_FILE = "BREAKPOINT_FILE";
        public static final String CL_TRANSACTION_LOG = "TRANSACTION_LOG";
        public static final String CL_LOB = "LOB";
        public static final String CL_BATCH = "BATCH";
        public static final String CL_DIRECTORY = "DIRECTORY";
        public static final String CL_FILE_RELATION = "FILE_DIRECTORY_REL";
        public static final String CL_CLASS = "CLASS";
        public static final String CL_ATTRIBUTE = "ATTRIBUTE";
        public static final String CL_CLASS_ATTR_REL = "CLASS_ATTR_REL";
    }
}
