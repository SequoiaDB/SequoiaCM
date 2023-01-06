package com.sequoiacm.common;

public class PropertiesDefine {
    public static String APPLICATION_PROPERTIES_LOCATION = "spring.config.location";
    public static String LOGGING_CONFIG = "logging.config";

    // *********************** ROOT SITE ******************************
    public static final String PROPERTY_ROOTSITE_URL = "scm.rootsite.meta.url";
    public static final String PROPERTY_ROOTSITE_USER = "scm.rootsite.meta.user";
    public static final String PROPERTY_ROOTSITE_PASSWD = "scm.rootsite.meta.password";

    // *********************** ROOT SITE NEW ******************************
    public static final String PROPERTY_ROOTSITE_URL_NEW = "scm.store.sequoiadb.urls";
    public static final String PROPERTY_ROOTSITE_USER_NEW = "scm.store.sequoiadb.username";
    public static final String PROPERTY_ROOTSITE_PASSWD_NEW = "scm.store.sequoiadb.password";

    // *********************** SEQUOIADB ******************************
    public static final String PROPERTY_SDB_CONNECTTIMEOUT = "scm.sdb.connectTimeout";
    public static final String PROPERTY_SDB_SOCKETTIMEOUT = "scm.sdb.socketTimeout";
    public static final String PROPERTY_SDB_MAXCONNECTRETRYTIME = "scm.sdb.maxAutoConnectRetryTime";
    public static final String PROPERTY_SDB_USENAGLE = "scm.sdb.useNagle";
    public static final String PROPERTY_SDB_USESSL = "scm.sdb.useSSL";
    public static final String PROPERTY_SDB_KEEPALIVETIME = "scm.sdb.keepAliveTime";
    public static final String PROPERTY_SDB_MAXCONNECTIONNUM = "scm.sdb.maxConnectionNum";
    public static final String PROPERTY_SDB_VALIDATECONNECTION = "scm.sdb.validateConnection";
    public static final String PROPERTY_SDB_DELTAINCCOUNT = "scm.sdb.deltaIncCount";
    public static final String PROPERTY_SDB_MAXIDLENUM = "scm.sdb.maxIdleNum";
    public static final String PROPERTY_SDB_RECHECKPERIOD = "scm.sdb.recheckCyclePeriod";
    public static final String PROPERTY_SDB_CONNECTSTRATEGY = "scm.sdb.connectStrategy";

    // *********************** SEQUOIADB NEW******************************
    public static final String PROPERTY_SDB_CONNECTTIMEOUT_NEW = "scm.store.sequoiadb.connectTimeout";
    public static final String PROPERTY_SDB_SOCKETTIMEOUT_NEW = "scm.store.sequoiadb.socketTimeout";
    public static final String PROPERTY_SDB_MAXCONNECTRETRYTIME_NEW = "scm.store.sequoiadb.maxAutoConnectRetryTime";
    public static final String PROPERTY_SDB_USENAGLE_NEW = "scm.store.sequoiadb.useNagle";
    public static final String PROPERTY_SDB_USESSL_NEW = "scm.store.sequoiadb.useSSL";
    public static final String PROPERTY_SDB_KEEPALIVETIME_NEW = "scm.store.sequoiadb.keepAliveTime";
    public static final String PROPERTY_SDB_MAXCONNECTIONNUM_NEW = "scm.store.sequoiadb.maxConnectionNum";
    public static final String PROPERTY_SDB_VALIDATECONNECTION_NEW = "scm.store.sequoiadb.validateConnection";
    public static final String PROPERTY_SDB_DELTAINCCOUNT_NEW = "scm.store.sequoiadb.deltaIncCount";
    public static final String PROPERTY_SDB_MAXIDLENUM_NEW = "scm.store.sequoiadb.maxIdleNum";
    public static final String PROPERTY_SDB_RECHECKPERIOD_NEW = "scm.store.sequoiadb.recheckCyclePeriod";
    public static final String PROPERTY_SDB_CONNECTSTRATEGY_NEW = "scm.store.sequoiadb.connectStrategy";

    // *********************** SERVER ******************************
    public static final String PROPERTY_SERVER_PORT = "server.port";
    public static final String PROPERTY_SERVER_TRANSFER_CHECK_LENGTH = "scm.server.transferCheckLength";
    public static final String PROPERTY_SERVER_TRANSFER_CONNECT_TIMEOUT = "scm.server.transferConnectTimeout";
    public static final String PROPERTY_SERVER_TRANSFER_READ_TIMEOUT = "scm.server.transferReadTimeout";
    public static final String PROPERTY_SERVER_LIST_INSTANCE_CHECK_INTERVAL = "scm.server.listInstanceCheckInterval";

    // *********************** zoo keeper ******************************
    public static final String PROPERTY_ZK_URL = "scm.zk.url";
    public static final String PROPERTY_ZK_LOCKTIMEOUT = "scm.zk.lockTimeout";
    public static final String PROPERTI_ZK_CLIENT_TIMEOUT = "scm.zk.clientTimeout";

    // *********************** privilege ******************************
    public static final String PROPERTY_PRIVILEGE_HBINTERVAL = "scm.privilege.heartbeat.interval";

    // *********************** audit ******************************
    public static final String PROPERTY_AUDIT_MASK = "scm.audit.mask";
    public static final String PROPERTY_AUDIT_USERMASK = "scm.audit.userMask";
    public static final String PROPERTY_AUDIT_USER = "scm.audit.user";
    public static final String PROPERTY_AUDIT_USERTYPE = "scm.audit.userType";

    // ***********************jvm ******************************
    // used by scmctl tools. do not use by scm node
    public static String PROPERTY_JVM_OPTIONS = "scm.jvm.options";

    // ************************scm process info*******************
    // keep consistent in manifest file
    public static final String PROPERTY_SCM_VERSION = "SCM-Version";
    // keep consistent in manifest file
    public static final String PROPERTY_SCM_COMPILE_TIME = "Build-Time";
    // keep consistent in manifest file
    public static final String PROPERTY_SCM_REVISION = "SCM-Revision";
    public static final String PROPERTY_SCM_STATUS = "Node-Status";
    public static final String PROPERTY_SCM_START_TIME = "Start-Time";

    // *************************spring properties*******************
    public static String PROPERTY_SCM_SPRING_APP_NAME = "spring.application.name";
    public static String PROPERTY_SCM_EUREKA_METADATA_IS_ROOTSITE = "eureka.instance.metadata-map.isRootSiteInstance";
    public static String PROPERTY_SCM_EUREKA_METADATA_SITE_ID = "eureka.instance.metadata-map.siteId";

    // *************************directory cache*******************
    public static final String PROPERTY_DIR_CACHE_ENABLE = "scm.dir.cache.enable";
    public static final String PROPERTY_DIR_CACHE_MAXSIZE = "scm.dir.cache.maxSize";
}
