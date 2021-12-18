package com.sequoiacm.contentserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;

@Component
@ConfigurationProperties(prefix = "scm.server")
public class ServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    private int transferCheckLength = CommonDefine.DefaultValue.TRANSFER_PERIOD_CHECK_LENGTH;
    private int transferConnectTimeout = 1000;
    private int transferReadTimeout = 30000;
    private int listInstanceCheckInterval = 2000;
    private int fulltextCreateTimeout = 10000;
    private int fileRenameBatchLockTimeout = 10000;
    private int maxConcurrentTask = 10;

    private Environment environment;
    private static final String PROPERTY_RIBBON_READ_TIMEOUT = "ribbon.ReadTimeout";
    private static final String PROPERTY_RIBBON_CONNECT_TIMEOUT = "ribbon.ConnectTimeout";
    private static final String PROPERTY_TRANSFER_READ_TIMEOUT = "scm.server.transferReadTimeout";
    private static final String PROPERTY_TRANSFER_CONNECT_TIMEOUT = "scm.server.transferConnectTimeout";

    @Value("${server.port}")
    private int serverPort;
    
    public ServerConfig(Environment environment) {
        this.environment = environment;
        this.transferConnectTimeout = getPreferredValue(PROPERTY_TRANSFER_CONNECT_TIMEOUT,
                PROPERTY_RIBBON_CONNECT_TIMEOUT, transferConnectTimeout);
        this.transferReadTimeout = getPreferredValue(PROPERTY_TRANSFER_READ_TIMEOUT,
                PROPERTY_RIBBON_READ_TIMEOUT, transferReadTimeout);
    }

    public int getListInstanceCheckInterval() {
        return listInstanceCheckInterval;
    }

    public int getFulltextCreateTimeout() {
        return fulltextCreateTimeout;
    }

    public void setFulltextCreateTimeout(int fulltextCreateTimeout) {
        this.fulltextCreateTimeout = fulltextCreateTimeout;
    }

    public void setListInstanceCheckInterval(int listInstanceCheckInterval) {
        this.listInstanceCheckInterval = listInstanceCheckInterval;
    }

    public int getTransferCheckLength() {
        return transferCheckLength;
    }

    public void setTransferCheckLength(int transferCheckLength) {
        this.transferCheckLength = transferCheckLength;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getTransferConnectTimeout() {
        return transferConnectTimeout;
    }

    public int getTransferReadTimeout() {
        return transferReadTimeout;
    }

    public void setTransferConnectTimeout(int transferConnectTimeout) {
        this.transferConnectTimeout = transferConnectTimeout;
    }

    public void setTransferReadTimeout(int transferReadTimeout) {
        this.transferReadTimeout = transferReadTimeout;
    }

    public int getFileRenameBatchLockTimeout() {
        return fileRenameBatchLockTimeout;
    }

    public void setFileRenameBatchLockTimeout(int fileRenameBatchLockTimeout) {
        this.fileRenameBatchLockTimeout = fileRenameBatchLockTimeout;
    }

    public int getMaxConcurrentTask() {
        return maxConcurrentTask;
    }

    public void setMaxConcurrentTask(int maxConcurrentTask) {
        if (maxConcurrentTask < 1) {
            logger.warn("reset scm.server.maxConcurrentTask: {} -> {}", maxConcurrentTask, 1);
            maxConcurrentTask = 1;
        }
        this.maxConcurrentTask = maxConcurrentTask;
    }

    private int getPreferredValue(String firstPropertyKey, String secondPropertyKey,
            int defaultValue) {
        String firstPropertyValue = environment.getProperty(firstPropertyKey);
        if (firstPropertyValue != null) {
            return Integer.parseInt(firstPropertyValue);
        }
        String secondPropertyValue = environment.getProperty(secondPropertyKey);
        if (secondPropertyValue != null) {
            logger.info("ServerConfig: {} is not exist, use {}, value:{}", firstPropertyKey,
                    secondPropertyKey, secondPropertyValue);
            return Integer.parseInt(secondPropertyValue);
        }
        return defaultValue;
    }
}
