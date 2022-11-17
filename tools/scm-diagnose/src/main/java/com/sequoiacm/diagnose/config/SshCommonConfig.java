package com.sequoiacm.diagnose.config;

public class SshCommonConfig {
    public static String privateKeyPath = "~/.ssh/id_rsa";
    public static Integer connectTimeout = 3 * 60 * 1000;

    public static String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public static void setPrivateKeyPath(String privateKeyPath) {
        SshCommonConfig.privateKeyPath = privateKeyPath;
    }

    public static Integer getConnectTimeout() {
        return connectTimeout;
    }

    public static void setConnectTimeout(int connectTimeout) {
        SshCommonConfig.connectTimeout = connectTimeout;
    }
}
