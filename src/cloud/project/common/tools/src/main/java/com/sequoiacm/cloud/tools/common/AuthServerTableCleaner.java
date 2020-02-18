package com.sequoiacm.cloud.tools.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.tools.exception.ScmToolsException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.Sequoiadb;

class AuthServerTableCleaner extends AuthServerTableProcessor implements ScmSysTableCleaner {
    private final static Logger logger = LoggerFactory.getLogger(AuthServerTableCreator.class);

    AuthServerTableCleaner(String sdbUrl, String sdbUser, String sdbPwdFile)
            throws ScmToolsException {
        super(sdbUrl, sdbUser, sdbPwdFile);
    }

    @Override
    public void clean() throws ScmToolsException {
        Sequoiadb sdb = getConnection();
        try {
            if (!sysCollectionSpaceExists(sdb)) {
                return;
            }

            CollectionSpace sysCS = getSysCollectionSpace(sdb);

            System.out.println("Dropping collection: " + CL_SESSIONS);
            logger.info("Dropping collection: " + CL_SESSIONS);
            dropCollectionIfExists(sysCS, CL_SESSIONS);

            System.out.println("Dropping collection: " + CL_USERS);
            logger.info("Dropping collection: " + CL_USERS);
            dropCollectionIfExists(sysCS, CL_USERS);

            System.out.println("Dropping collection: " + CL_ROLES);
            logger.info("Dropping collection: " + CL_ROLES);
            dropCollectionIfExists(sysCS, CL_ROLES);

            System.out.println("Dropping collection: " + CL_PRIV_VERSION);
            logger.info("Dropping collection: " + CL_PRIV_VERSION);
            dropCollectionIfExists(sysCS, CL_PRIV_VERSION);

            System.out.println("Dropping collection: " + CL_PRIV_RESOURCE);
            logger.info("Dropping collection: " + CL_PRIV_RESOURCE);
            dropCollectionIfExists(sysCS, CL_PRIV_RESOURCE);

            System.out.println("Dropping collection: " + CL_PRIV_ROLE_RESOURCE_REL);
            logger.info("Dropping collection: " + CL_PRIV_ROLE_RESOURCE_REL);
            dropCollectionIfExists(sysCS, CL_PRIV_ROLE_RESOURCE_REL);
        }
        finally {
            releaseConnection(sdb);
        }
    }
}
