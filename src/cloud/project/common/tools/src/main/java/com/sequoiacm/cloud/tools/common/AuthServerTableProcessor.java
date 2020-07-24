package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

abstract class AuthServerTableProcessor extends ScmSysTableProcessor {

    final static String CL_SESSIONS = "SESSIONS";
    final static String CL_USERS = "USERS";
    final static String CL_ROLES = "ROLES";
    final static String CL_PRIV_VERSION = "PRIV_VERSION";
    final static String CL_PRIV_RESOURCE = "PRIV_RESOURCE";
    final static String CL_PRIV_ROLE_RESOURCE_REL = "PRIV_ROLE_RESOURCE_REL";

    protected AuthServerTableProcessor(String sdbUrl, String username, String passwordFile)
            throws ScmToolsException {
        super(sdbUrl, username, passwordFile);
    }

    protected AuthServerTableProcessor(Properties properties) throws ScmToolsException {
        super(properties);
    }
}
