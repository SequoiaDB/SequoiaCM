package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.tools.exception.ScmToolsException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.Sequoiadb;

class AdminServerTableCreator extends AdminServerTableProcessor implements ScmSysTableCreator {
    private final static Logger logger = LoggerFactory.getLogger(AdminServerTableCreator.class);

    AdminServerTableCreator(Properties properties) throws ScmToolsException {
        super(properties);
    }

    @Override
    public void create() throws ScmToolsException {
        Sequoiadb sdb = getConnection();
        try {
            CollectionSpace sysCS = ensureSysCollectionSpace(sdb);

            System.out.println("Creating collection: " + CL_TRAFFIC);
            logger.info("Creating collection: " + CL_TRAFFIC);
            createCollectionIfNotExist(sysCS, CL_TRAFFIC);

            System.out.println("Creating collection: " + CL_FILE_DELTA);
            logger.info("Creating collection: " + CL_FILE_DELTA);
            createCollectionIfNotExist(sysCS, CL_FILE_DELTA);
        }
        finally {
            releaseConnection(sdb);
        }
    }
}
