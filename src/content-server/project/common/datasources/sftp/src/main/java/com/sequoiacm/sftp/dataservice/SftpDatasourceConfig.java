package com.sequoiacm.sftp.dataservice;

import java.util.Map;

public class SftpDatasourceConfig {

    private static final String KEY_CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
    private static final String KEY_SOCKET_TIMEOUT = "SOCKET_TIMEOUT";
    private static final String KEY_SERVER_ALIVE_INTERVAL = "SERVER_ALIVE_INTERVAL";

    private static int connectTimeout = 30000;
    private static int socketTimeout = 30000;
    private static int serverAliveInterval = 2000;

    public static void init(Map<String, String> dataConf) {
        if (dataConf == null) {
            return;
        }
        String tmp = dataConf.get(KEY_CONNECT_TIMEOUT);
        if (tmp != null) {
            connectTimeout = Integer.parseInt(tmp);
        }

        tmp = dataConf.get(KEY_SOCKET_TIMEOUT);
        if (tmp != null) {
            socketTimeout = Integer.parseInt(tmp);
        }

        tmp = dataConf.get(KEY_SERVER_ALIVE_INTERVAL);
        if (tmp != null) {
            serverAliveInterval = Integer.parseInt(tmp);
        }
    }

    public static int getConnectTimeout() {
        return connectTimeout;
    }

    public static int getSocketTimeout() {
        return socketTimeout;
    }

    public static int getServerAliveInterval() {
        return serverAliveInterval;
    }
}
