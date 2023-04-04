package com.sequoiacm.infrastructure.config.core.common;

import com.sequoiacm.common.FieldName;

public class ScmRestArgDefine {
    public static final String CONFIG_RESP_BODY = "config_resp_body";

    public static final String CONFIG = "config";
    public static final String OPTION = "option";
    public static final String FILTER = "filter";
    public static final String SERVICE_NAME = "service_name";
    public static final String CONFIG_NAME = "config_name";
    public static final String EVENT_TYPE = "event_type";
    public static final String IS_ASYNC_NOTIFY = "is_async_notify";

    public static final String WORKSPACE_CONF_WORKSPACENAME = "name";
    public static final String WORKSPACE_CONF_WORKSPACEID = "id";
    public static final String WORKSPACE_CONF_DESCRIPTION = "description";
    public static final String WORKSPACE_CONF_PREFERRED = "preferred";
    public static final String WORKSPACE_CONF_SITE_CACHE_STRATEGY = "site_cache_strategy";
    public static final String WORKSPACE_UPDATOR_ENABLE_DIRECTORY = "enable_directory";
    public static final String WORKSPACE_CONF_METALOCATION = "meta_location";
    public static final String WORKSPACE_CONF_DATALOCATION = "data_location";
    public static final String WORKSPACE_CONF_CREATE_USER = "create_user";
    public static final String WORKSPACE_CONF_UPDATE_USER = "update_user";
    public static final String WORKSPACE_CONF_UPDATE_TIME = "update_time";
    public static final String WORKSPACE_CONF_CREATE_TIME = "create_time";
    public static final String WORKSPACE_CONF_WORKSPACEVERSION = "workspace_version";
    public static final String WORKSPACE_CONF_UPDATOR = "updator";
    public static final String WORKSPACE_CONF_ADD_DATALOCATION = "add_datalocation";
    public static final String WORKSPACE_CONF_REMOVE_DATALOCATION = "remove_site";
    public static final String WORKSPACE_CONF_UPDATE_DATALOCATION = "update_datalocation";
    public static final String WORKSPACE_CONF_MATCHER = "matcher";
    public static final String WORKSPACE_CONF_OLD_WS = "old_workspace";
    public static final String WORKSPACE_CONF_EXTERNAL_DATA = "external_data";

    public static final String SITE_CONF_SITENAME = "name";
    public static final String SITE_CONF_SITEVERSION = "site_version";
    public static final String SITE_CONF_UPDATOR = "updator";
    public static final String SITE_CONF_STAGETAG = "stage_tag";

    public static final String NODE_CONF_NODENAME = "name";
    public static final String NODE_CONF_NODEHOSTNAME = "host_name";
    public static final String NODE_CONF_NODEPORT = "port";
    public static final String NODE_CONF_NODEVERSION = "node_version";

    public static final String USER_CONF_USERNAME = FieldName.User.FIELD_USERNAME;

    public static final String ROLE_CONF_ROLENAME = FieldName.Role.FIELD_ROLE_NAME;

    public static final String META_DATA_CONF_TYPE_CLASS = "config_class";
    public static final String META_DATA_CONF_TYPE_ATTRIBUTE = "config_attribute";
    public static final String META_DATA_CLASS_ID = "class_id";
    public static final String META_DATA_ATTRIBUTE_ID = "attribute_id";
    public static final String META_DATA_WORKSPACE_NAME = "workspace_name";
    public static final String META_DATA_VERSION = "version";
    public static final String META_DATA_ATTACH_ATTRUBUTE_ID = "attach_attribute_id";
    public static final String META_DATA_DETTACH_ATTRUBUTE_ID = "dettach_attribute_id";
    public static final String META_DATA_CLASS_NAME = "class_name";
    public static final String META_DATA_CLASS_DESCRIPTION = "class_description";
    public static final String META_DATA_ATTRIBUTE_DESCRIPTION = "atrribute_description";
    public static final String META_DATA_ATTRIBUTE_DISPLAY_NAME = "atrribute_display_name";
    public static final String META_DATA_ATTRIBUTE_CHECK_RULE = "atrribute_check_rule";
    public static final String META_DATA_ATTRIBUTE_REQUIRE = "atrribute_require";
    public static final String META_DATA_UPDATE_USER = "update_user";

    public static final String CONF_VERSION_BUSINESS_TYPE = "business_type";
    public static final String CONF_VERSION_BUSINESS_NAME = "business_name";
    public static final String CONF_VERSION_BUSINESS_VERSION = "business_version";
    public static final String CONF_VERSION_LIMIT = "limit";

    public static final String CONF_PROPS_TARGET_TYPE = "target_type";
    public static final String CONF_PROPS_TARGETS = "targets";
    public static final String CONF_PROPS_UPDATE_PROPERTIES = "update_properties";
    public static final String CONF_PROPS_DELETE_PROPERTIES = "delete_properties";
    public static final String CONF_PROPS_ACCEPT_UNKNOWN_PROPS = "accept_unknown_props";
    public static final String CONF_PROPS_RES_SET = "update_conf_result";
    public static final String CONF_PROPS_RES_SET_SUCCESS = "successes";
    public static final String CONF_PROPS_RES_SET_FAILES = "failes";
    public static final String CONF_PROPS_RES_SERVICE = "service";
    public static final String CONF_PROPS_RES_INSTANCE = "instance";
    public static final String CONF_PROPS_RES_MESSAGE = "message";
    public static final String CONF_PROPS_REBOOT_CONF = "reboot_conf";
    public static final String CONF_PROPS_ADJUST_CONF = "adjust_conf";

    public static final String BUCKET_CONF_VERSION = "bucket_version";
    public static final String BUCKET_CONF_GLOBAL_VERSION = "bucket_global_version";
    public static final String BUCKET_CONF_FILTER_TYPE = "type";
    public static final String BUCKET_CONF_FILTER_NAME = "name";
    public static final String BUCKET_CONF_FILTER_MATCHER = "matcher";
    public static final String BUCKET_CONF_FILTER_ORDERBY = "orderby";
    public static final String BUCKET_CONF_FILTER_SKIP = "skip";
    public static final String BUCKET_CONF_FILTER_LIMIT = "limit";

    public static final String COUNT_HEADER = "X-SCM-Count";
}
