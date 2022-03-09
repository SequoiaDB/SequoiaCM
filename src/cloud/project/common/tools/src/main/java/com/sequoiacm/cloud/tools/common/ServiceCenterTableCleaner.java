package com.sequoiacm.cloud.tools.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.Sequoiadb;

class ServiceCenterTableCleaner extends ServiceCenterTableProcessor implements ScmSysTableCleaner {
    private final static Logger logger = LoggerFactory.getLogger(AdminServerTableCreator.class);

    ServiceCenterTableCleaner(String sdbUrl, String username, String passwordFile)
            throws ScmToolsException {
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

            System.out.println("Dropping collection: " + CL_INSTANCE);
            logger.info("Dropping collection: " + CL_INSTANCE);
            dropCollectionIfExists(sysCS, CL_INSTANCE);
        }
        finally {
            releaseConnection(sdb);
        }
    }
}
