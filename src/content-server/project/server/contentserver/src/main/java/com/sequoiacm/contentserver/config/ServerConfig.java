package com.sequoiacm.contentserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;

@Component
@ConfigurationProperties(prefix = "scm.server")
public class ServerConfig {
    private int transferCheckLength = CommonDefine.DefaultValue.TRANSFER_PERIOD_CHECK_LENGTH;
    private int transferConnectTimeout = 30000;
    private int transferReadTimeout = 120000;
    private int listInstanceCheckInterval = 2000;
    private int fulltextCreateTimeout = 10000;

    @Value("${server.port}")
    private int serverPort;

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

}
