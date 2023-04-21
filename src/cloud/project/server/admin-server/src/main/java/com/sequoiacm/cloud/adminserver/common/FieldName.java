package com.sequoiacm.cloud.adminserver.common;

public class FieldName {
    public static final class File {
        private File() {
        }
        public static final String FIELD_ID = "id";
        public static final String FIELD_SIZE = "size";        //long
        public static final String FIELD_CREATE_MONTH = "create_month";
        public static final String FIELD_CREATE_TIME = "create_time";
    }

    public static final class Site {
        private Site() {
        }
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_ROOT_FLAG = "root_site_flag";
    }

    public static final class Workspace {
        private Workspace() {
        }
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_SITE_ID = "site_id";
    }
    
    public static final class ContentServer {
        private ContentServer() {
        }
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_SITE_ID = "site_id";
        public static final String FIELD_HOST_NAME = "host_name";
        public static final String FIELD_PORT = "port";
    }
    
    public static final class Traffic {
        private Traffic() {
        }
        public static final String FIELD_TYPE           = "type";
        public static final String FIELD_WORKSPACE_NAME = "workspace_name";
        public static final String FIELD_TRAFFIC        = "traffic";
        public static final String FIELD_RECORD_TIME    = "record_time";
    }
    
    public static final class FileDelta {
        private FileDelta() {
        }
        public static final String FIELD_WORKSPACE_NAME = "workspace_name";
        public static final String FIELD_COUNT_DELTA    = "count_delta";
        public static final String FIELD_SIZE_DELTA     = "size_delta";
        public static final String FIELD_RECORD_TIME    = "record_time";
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

}