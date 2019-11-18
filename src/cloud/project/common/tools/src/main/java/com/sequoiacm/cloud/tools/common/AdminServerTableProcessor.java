package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

abstract class AdminServerTableProcessor extends ScmSysTableProcessor {

    final static String CL_TRAFFIC = "TRAFFIC";
    final static String CL_FILE_DELTA = "FILE_DELTA";

    protected AdminServerTableProcessor(String sdbUrl, String username, String passwordFile) {
        super(sdbUrl, username, passwordFile);
    }

    protected AdminServerTableProcessor(Properties properties) {
        super(properties);
    }
}
