package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

class AuthServerTableCreator extends AuthServerTableProcessor implements ScmSysTableCreator {
    private final static Logger logger = LoggerFactory.getLogger(AuthServerTableCreator.class);

    AuthServerTableCreator(Properties properties) throws ScmToolsException {
        super(properties);
    }

    @Override
    public void create() throws ScmToolsException {
        Sequoiadb sdb = getConnection();
        try {
            CollectionSpace sysCS = ensureSysCollectionSpace(sdb);

            System.out.println("Creating collection: " + CL_SESSIONS);
            logger.info("Creating collection: " + CL_SESSIONS);
            createCollectionIfNotExist(sysCS, CL_SESSIONS);

            System.out.println("Creating collection: " + CL_USERS);
            logger.info("Creating collection: " + CL_USERS);
            createCollectionIfNotExist(sysCS, CL_USERS);

            System.out.println("Creating collection: " + CL_ROLES);
            logger.info("Creating collection: " + CL_ROLES);
            createCollectionIfNotExist(sysCS, CL_ROLES);

            System.out.println("Creating collection: " + CL_PRIV_VERSION);
            logger.info("Creating collection: " + CL_PRIV_VERSION);
            DBCollection clPrivVersion = createCollectionIfNotExist(sysCS, CL_PRIV_VERSION);
            SdbHelper.insert(clPrivVersion, new BasicBSONObject("version", 1));

            System.out.println("Creating collection: " + CL_PRIV_RESOURCE);
            logger.info("Creating collection: " + CL_PRIV_RESOURCE);
            createCollectionIfNotExist(sysCS, CL_PRIV_RESOURCE);

            System.out.println("Creating collection: " + CL_PRIV_ROLE_RESOURCE_REL);
            logger.info("Creating collection: " + CL_PRIV_ROLE_RESOURCE_REL);
            createCollectionIfNotExist(sysCS, CL_PRIV_ROLE_RESOURCE_REL);
        }
        finally {
            releaseConnection(sdb);
        }
    }
}
