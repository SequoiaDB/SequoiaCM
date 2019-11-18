package com.sequoiacm.deploy.config;

import java.io.FileInputStream;
import java.util.Properties;

public class SystemApplicationProperty {
    private Properties p;
    private static volatile SystemApplicationProperty instance;

    public static SystemApplicationProperty getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SystemApplicationProperty.class) {
            if (instance != null) {
                return instance;
            }
            instance = new SystemApplicationProperty();
            return instance;
        }
    }

    public SystemApplicationProperty() {
        p = new Properties();
        String filePath = System.getProperty("application.properties");
        if (filePath != null) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(filePath);
                p.load(is);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("failed to parse application file:" + filePath,
                        e);
            }
        }

        p.putAll(System.getProperties());
    }

    public Integer getInt(String key, Integer defaultV) {
        Integer v = getInt(key);
        if (v == null) {
            return defaultV;
        }
        return v;
    }

    public Integer getInt(String key) {
        if (p == null) {
            return null;
        }
        String v = p.getProperty(key);
        if (v == null) {
            return null;
        }
        return Integer.valueOf(v);
    }

    public String getString(String key, String defaultV) {
        String v = getString(key);
        if (v == null) {
            return defaultV;
        }
        return v;
    }

    public String getString(String key) {
        if (p == null) {
            return null;
        }
        String v = p.getProperty(key);
        if (v == null) {
            return null;
        }
        return v;
    }
}
