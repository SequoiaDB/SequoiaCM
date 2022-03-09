package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

abstract class ServiceCenterTableProcessor extends ScmSysTableProcessor {

    final static String CL_INSTANCE = "EUREKA_INSTANCE";

    protected ServiceCenterTableProcessor(String sdbUrl, String username, String passwordFile)
            throws ScmToolsException {
        super(sdbUrl, username, passwordFile);
    }

    protected ServiceCenterTableProcessor(Properties properties) throws ScmToolsException {
        super(properties);
    }
}
