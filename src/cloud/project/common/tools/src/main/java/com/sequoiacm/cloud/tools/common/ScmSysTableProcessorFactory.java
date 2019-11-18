package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import com.sequoiacm.cloud.tools.element.ScmNodeType;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public final class ScmSysTableProcessorFactory {
    private ScmSysTableProcessorFactory() {
    }

    public static ScmSysTableCreator getSysTableCreator(ScmNodeType nodeType, Properties properties)
            throws ScmToolsException {
        switch (nodeType) {
            case AUTH_SERVER:
                return new AuthServerTableCreator(properties);
            case ADMIN_SERVER:
                return new AdminServerTableCreator(properties);
            default:
                return null;
        }
    }

    public static ScmSysTableCleaner getSysTableCleaner(ScmNodeType nodeType, String sdbUrl,
            String sdbUser, String sdbPwdFile) {
        switch (nodeType) {
            case AUTH_SERVER:
                return new AuthServerTableCleaner(sdbUrl, sdbUser, sdbPwdFile);
            case ADMIN_SERVER:
                return new AdminServerTableCleaner(sdbUrl, sdbUser, sdbPwdFile);
            default:
                return null;
        }
    }
}
