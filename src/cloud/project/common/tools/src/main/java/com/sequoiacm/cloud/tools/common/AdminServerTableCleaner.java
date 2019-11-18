package com.sequoiacm.cloud.tools.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.tools.exception.ScmToolsException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.Sequoiadb;

class AdminServerTableCleaner extends AdminServerTableProcessor implements ScmSysTableCleaner {
    private final static Logger logger = LoggerFactory.getLogger(AdminServerTableCreator.class);

    AdminServerTableCleaner(String sdbUrl, String username, String passwordFile) {
        super(sdbUrl, username, passwordFile);
    }

    @Override
    public void clean() throws ScmToolsException {
        Sequoiadb sdb = getConnection();
        try {
            if (!sysCollectionSpaceExists(sdb)) {
                return;
            }

            CollectionSpace sysCS = getSysCollectionSpace(sdb);

            System.out.println("Dropping collection: " + CL_TRAFFIC);
            logger.info("Dropping collection: " + CL_TRAFFIC);
            dropCollectionIfExists(sysCS, CL_TRAFFIC);

            System.out.println("Dropping collection: " + CL_FILE_DELTA);
            logger.info("Dropping collection: " + CL_FILE_DELTA);
            dropCollectionIfExists(sysCS, CL_FILE_DELTA);

        } finally {
            releaseConnection(sdb);
        }
    }
}
