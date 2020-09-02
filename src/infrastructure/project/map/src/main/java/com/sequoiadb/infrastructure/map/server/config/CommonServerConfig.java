package com.sequoiadb.infrastructure.map.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.server")
public class CommonServerConfig {
    private int listInstanceCheckInterval = 20000;

    public void setListInstanceCheckInterval(int listInstanceCheckInterval) {
        this.listInstanceCheckInterval = listInstanceCheckInterval;
    }

    public int getListInstanceCheckInterval() {
        return listInstanceCheckInterval;
    }

}
