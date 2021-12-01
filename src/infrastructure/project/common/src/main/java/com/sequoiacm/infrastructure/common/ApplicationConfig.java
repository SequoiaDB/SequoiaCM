package com.sequoiacm.infrastructure.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ApplicationConfig {
    private static Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static ApplicationConfig instance;
    private Map<String, String> configMap = new HashMap<String, String>();

    private ApplicationConfig() throws Exception {
        init();
    }

    public static ApplicationConfig getInstance() throws Exception {
        if (instance == null) {
            synchronized (ApplicationConfig.class) {
                if (instance == null) {
                    instance = new ApplicationConfig();
                }
            }
        }
        return instance;
    }

    private void init() throws Exception {
        Properties prop = new Properties();
        InputStream is = null;
        FileInputStream fis = null;
        String confRelativePath = "/application.properties";
        try {
            // 首先加载jar包内部的配置文件
            is = ApplicationConfig.class.getResourceAsStream(confRelativePath);
            if (is == null) {
                logger.info("no resource with this name is found in jar, config file name:{}", confRelativePath);
            }
            else {
                prop.load (is);
                for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                    configMap.put((String)entry.getKey(), (String)entry.getValue());
                }
            }
            // 其次加载jar包外部的配置文件
            // property "spring.config.location" is autoConfig by server-common/resources/META-INF/spring.factories
            confRelativePath = System.getProperty("spring.config.location");
            logger.info("spring.config.location={}", confRelativePath);
            fis = new FileInputStream(confRelativePath);
            prop.load(fis);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                configMap.put((String)entry.getKey(), (String)entry.getValue());
            }
        }
        catch (FileNotFoundException e) {
            logger.info("no resource with this name is found, config file name:{}", confRelativePath);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (is != null) {
                is.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }


    public String getConfig(String configKey) {
        return configMap.get(configKey);
    }

    public String getConfig(String configKey, String defaultValue) {
        String configValue = configMap.get(configKey);
        return configValue == null ? defaultValue : configValue;
    }
}
