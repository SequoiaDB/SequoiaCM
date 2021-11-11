package com.sequoiacm.daemon.common;

import java.io.File;

public class DaemonDefine {
    public static final String MAIN_METHOD = "com.sequoiacm.daemon.Scmd";

    public static final String CRON_PROPERTIES = ".crontab.properties";
    public static final String SCMD_PROPERTIES = ".scmd.properties";
    public static final String MONITOR_TABLE = "monitor-table.json";
    public static final String MONITOR_BACKUP = "monitor-table.json.b";
    public static final String CRON_LOG_CONF = "logback_cron.xml";
    public static final String ZOO_PATTERN = "zoo[0-9]*.cfg";
    public static final String ZOO_PID_FILE = "zookeeper_server.pid";
    public static final String ERROR_OUT = "error.out";

    public static final String SCMD_LOG_PATH = "." + File.separator + "log" + File.separator + "scmd.log";

    public static final String CONF = "conf";
    public static final String JARS = "jars";

    public static final String ENCODE_TYPE = "utf-8";

    public static final String DAEMON_LOCATION = "daemonHomePath";
    public static final String CLIENT_PORT = "clientPort";
    public static final String ZK_DATA_DIR = "dataDir";
    public static final String SERVER_PORT = "server.port";
    public static final String CRON_USER = "user";
    public static final String CRON_LINUX = "linuxCron";
    public static final String CRON_PERIOD = "period";

    public static final String USER_DIR = "user.dir";
    public static final String USER_NAME = "user.name";

    public static final String OPT_SHORT_PORT = "p";
    public static final String OPT_LONG_PORT = "port";
    public static final String OPT_SHORT_TYPE = "t";
    public static final String OPT_LONG_TYPE = "type";
    public static final String OPT_SHORT_STATUS = "s";
    public static final String OPT_LONG_STATUS = "status";
    public static final String OPT_SHORT_CONF = "c";
    public static final String OPT_LONG_CONF = "conf";
    public static final String OPT_SHORT_OVERWRITE = "o";
    public static final String OPT_LONG_OVERWRITE = "overwrite";
    public static final String OPT_SHORT_PERIOD = "p";
    public static final String OPT_LONG_PERIOD = "period";

    public static final int PERIOD_DEFAULT = 5;
    // PERIOD_MAXIMUM = 24 * 60 * 60 * 2
    public static final int PERIOD_MAXIMUM = 172800;
    public static final String NODE_STATUS_ON = "on";
    public static final String NODE_STATUS_OFF = "off";

    public static final String SERVER_TYPE = "server_type";
    public static final String PORT = "port";
    public static final String STATUS = "status";
    public static final String CONF_PATH = "conf_path";

    public static final String EXPORT_IGNORE_DAEMON_ENV = "export IGNORE_DAEMON=true;";

    public static final int MATCH_PORT_FLAG = 0x00000001;
    public static final int MATCH_TYPE_FLAG = 0x00000010;
    public static final int MATCH_ALL_FLAG = 0x11111111;
}
