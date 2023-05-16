package com.sequoiacm.tools.tag.common;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class UpgradeConfig {

    private static volatile UpgradeConfig config;
    private final Properties props = new Properties();

    private UpgradeConfig() {
        try {
            String confFile = "./conf/scm-upgrade-conf.properties";
            if (new File(confFile).exists()) {
                try (FileInputStream fileInputStream = new FileInputStream(confFile)) {
                    props.load(fileInputStream);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("load config file failed", e);
        }
    }

    public static UpgradeConfig getInstance() {
        if (config == null) {
            synchronized (UpgradeConfig.class) {
                if (config == null) {
                    config = new UpgradeConfig();
                }
            }
        }
        return config;
    }

    public String getConf(String key, String defaultValue) {
        return props.getProperty(key);
    }

    public int getIntConf(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
