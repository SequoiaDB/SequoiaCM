package com.sequoiacm.infrastructure.config.core.common;

public class FieldName {
    // CL_WORKSPACE
    public static final String FIELD_CLWORKSPACE_ID = "id";
    public static final String FIELD_CLWORKSPACE_NAME = "name"; // string, w1
    public static final String FIELD_CLWORKSPACE_META_LOCATION = "meta_location"; // object,
    public static final String FIELD_CLWORKSPACE_DATA_LOCATION = "data_location"; // object,
    public static final String FIELD_CLWORKSPACE_LOCATION_DOMAIN = "domain";
    public static final String FIELD_CLWORKSPACE_LOCATION_SITE_ID = "site_id";
    public static final String FIELD_CLWORKSPACE_CREATEUSER = "create_user";
    public static final String FIELD_CLWORKSPACE_CREATETIME = "create_time";
    public static final String FIELD_CLWORKSPACE_UPDATEUSER = "update_user";
    public static final String FIELD_CLWORKSPACE_UPDATETIME = "update_time";
    public static final String FIELD_CLWORKSPACE_DESCRIPTION = "description";
    public static final String FIELD_CLWORKSPACE_META_OPTIONS = "meta_options";
    public static final String FIELD_CLWORKSPACE_META_CS_OPTIONS = "collection_space";


    // CL_FILE & CL_FILE_HISTORY (file & document)
    public static final String FIELD_CLFILE_ID = "id"; // string, fileid
    public static final String FIELD_CLFILE_DIRECTORY_ID = "directory_id"; // string,
    public static final String FIELD_CLFILE_INNER_CREATE_MONTH = "create_month"; // string
    public static final String FIELD_CLFILE_MAJOR_VERSION = "major_version"; // int,
    public static final String FIELD_CLFILE_MINOR_VERSION = "minor_version"; // int,

    // CL_FILE_DIRECTORY_REL
    public static final String FIELD_CLREL_FILEID = "file_id";
    public static final String FIELD_CLREL_FILENAME = "file_name";
    public static final String FIELD_CLREL_DIRECTORY_ID = FIELD_CLFILE_DIRECTORY_ID;

    // CL_DIRECTORY
    public static final String FIELD_CLDIR_ID = "id";
    public static final String FIELD_CLDIR_NAME = "name";
    public static final String FIELD_CLDIR_USER = "user";
    public static final String FIELD_CLDIR_CREATE_TIME = "create_time";
    public static final String FIELD_CLDIR_UPDATE_TIME = "update_time";
    public static final String FIELD_CLDIR_UPDATE_USER = "update_user";
    public static final String FIELD_CLDIR_PARENT_DIRECTORY_ID = "parent_directory_id";

    // CL_CLASS
    public static final String FIELD_CLCLASS_NAME = "name";

    // CL_ATTR
    public static final String FIELD_CLATTR_NAME = "name";

    // CL_CLASS_ATTR_REL
    public static final String FIELD_CL_CLASS_ATTR_REL_CLASS_ID = "class_id";
    public static final String FIELD_CL_CLASS_ATTR_REL_ATTR_ID = "attr_id";

    // CL_BREAKPOINT_FILE
    public static final String FIELD_CLBREAKPOINTFILE_FILE_NAME = "file_name";

    // CL_BATCH
    public static final String FIELD_CLBATCH_ID = "id";

    // CL_CONTENTSERVER_CONF_VERSION
    public static final String FIELD_CLVERSION_BUSINESS_TYPE = "business_type";
    public static final String FIELD_CLVERSION_BUSINESS_NAME = "business_name";
    public static final String FIELD_CLVERSION_BUSINESS_VERSION = "business_version";

    // CL_SUBSCRIBER
    public static final String FIELD_CLSUBSCRIBER_CONFIG_NAME = "config_name";
    public static final String FIELD_CLSUBSCRIBER_SERVICE_NAME = "service_name";

    // CL_SITE
    public static final String FIELD_CLSITE_SITE_ID = "id";
    public static final String FIELD_CLSITE_SITE_NAME = "name";
    public static final String FIELD_CLSITE_SITE_ROOT_SITE_FLAG = "root_site_flag";
    public static final String FIELD_CLSITE_SITE_DATA = "data";
    public static final String FIELD_CLSITE_SITE_DATA_TYPE = "type";//type:sequoiadb/hbase
    public static final String FIELD_CLSITE_SITE_DATA_USER = "user";
    public static final String FIELD_CLSITE_SITE_DATA_PASSWORD = "password";
    public static final String FIELD_CLSITE_SITE_DATA_URL= "url";
    public static final String FIELD_CLSITE_SITE_DATA_CONF= "configuration"; //for hdfs|hbase
    public static final String FIELD_CLSITE_SITE_META = "meta";
    public static final String FIELD_CLSITE_SITE_META_USER = "user";
    public static final String FIELD_CLSITE_SITE_META_PASSWORD = "password";
    public static final String FIELD_CLSITE_SITE_META_URL= "url";

    // CL_CONTENT_SERVER
    public static final String FIELD_CLCONTENT_SERVER_ID = "id";
    public static final String FIELD_CLCONTENT_SERVER_NAME = "name";
    public static final String FIELD_CLCONTENT_SERVER_TYPE = "type";
    public static final String FIELD_CLCONTENT_SERVER_SITE_ID = "site_id";
    public static final String FIELD_CLCONTENT_SERVER_HOST_NAME = "host_name";
    public static final String FIELD_CLCONTENT_SERVER_PORT = "port";

    // CL DATA_TABLE_NAME_HOISTORY
    public static final String FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_NAME = "workspace_name";
    public static final String FIELD_CLTABLE_NAME_HISTORY_SITE_NAME = "site_name";
    public static final String FIELD_CLTABLE_NAME_HISTORY_WORKSPACE_IS_DELTED = "workspace_is_deleted";
    public static final String FIELD_CLTABLE_NAME_HISTORY_TABLE_CREATE_TIME = "table_create_time";
    public static final String FIELD_CLTABLE_NAME_HISTORY_TABLE_NAME = "table_name";

    public static class ClassTable {
        public static final String FIELD_ID = "id";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_DESCRIPTION = "description";
        public static final String FIELD_INNER_CREATE_USER = "create_user";
        public static final String FIELD_INNER_CREATE_TIME = "create_time";
        public static final String FIELD_INNER_UPDATE_USER = "update_user";
        public static final String FIELD_INNER_UPDATE_TIME = "update_time";
    }

    public static class AttributeTable {
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
        public static final String FIELD_CLASS_ID = "class_id";
        public static final String FIELD_ATTR_ID = "attr_id";
    }
}
