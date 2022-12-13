package com.sequoiacm.diagnose.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshCommonConfig {
    private static String privateKeyPath = "~/.ssh/id_rsa";
    private static Integer connectTimeout = 3 * 60 * 1000;
    private static final String PRIVATE_KEY_PATH = "private-key-path";
    private static final String CONNECT_TIMEOUT = "connect-timeout";
    private static final Logger logger = LoggerFactory.getLogger(SshCommonConfig.class);

    public static void assignmentCollectConfig(String key, String value) {
        switch (key) {
            case PRIVATE_KEY_PATH:
                privateKeyPath = value;
                break;
            case CONNECT_TIMEOUT:
                try {
                    connectTimeout = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, connect-timeout=" + value
                                    + ", it must be a number and < " + Integer.MAX_VALUE);
                }
                if (connectTimeout < 1) {
                    throw new IllegalArgumentException(
                            "collectConfig illegal configuration, connect-timeout=" + value
                                    + ", it must > 0");
                }
                break;
            default:
                logger.warn("collectConfig illegal configuration, " + key + "=" + value);
                break;
        }
    }

    public static String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public static Integer getConnectTimeout() {
        return connectTimeout;
    }

}