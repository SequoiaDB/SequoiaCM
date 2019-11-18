package com.sequoiacm.cloud.adminserver.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.adminserver.AdminServerConfig;

@Component
public class PropertiesUtils {

    private static AdminServerConfig adminServerConfig;
    
    @Autowired
    private void setAdminServerConfig(AdminServerConfig adminServerConfig) {
        PropertiesUtils.adminServerConfig = adminServerConfig;
    }
    
    public static int getListInstanceCheckInterval() {
        return adminServerConfig.getListInstanceCheckInterval();
    }
}
