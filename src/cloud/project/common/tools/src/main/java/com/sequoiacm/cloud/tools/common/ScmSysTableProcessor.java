package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import org.springframework.util.Assert;

import com.sequoiacm.cloud.tools.exception.ScmToolsException;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

abstract class ScmSysTableProcessor {
    private final static String SYS_CS = "SCMSYSTEM";

    private String sdbUrl;
    private String username;
    private String password;

    private final static String FIELD_SDB_URLS = "scm.store.sequoiadb.urls";
    private final static String FIELD_SDB_USERNAME = "scm.store.sequoiadb.username";
    private final static String FIELD_SDB_PASSWORD = "scm.store.sequoiadb.password";


    protected ScmSysTableProcessor(String sdbUrl, String username, String password) {
        Assert.hasText(sdbUrl, "Invalid SequoiaDB URL");
        this.sdbUrl = sdbUrl;
        this.username = username;
        this.password = password;
    }

    protected ScmSysTableProcessor(Properties properties) {
        this(properties.getProperty(FIELD_SDB_URLS), properties.getProperty(FIELD_SDB_USERNAME),
                properties.getProperty(FIELD_SDB_PASSWORD));
    }

    protected Sequoiadb getConnection() throws ScmToolsException {
        AuthInfo auth = ScmFilePasswordParser.parserFile(password);
        return SdbHelper.connectUrls(sdbUrl, username, auth.getPassword());
    }

    protected void releaseConnection(Sequoiadb sdb) {
        SdbHelper.closeCursorsAndConnection(sdb);
    }

    protected boolean sysCollectionSpaceExists(Sequoiadb sdb) {
        return sdb.isCollectionSpaceExist(SYS_CS);
    }

    protected CollectionSpace getSysCollectionSpace(Sequoiadb sdb) {
        return sdb.getCollectionSpace(SYS_CS);
    }

    protected CollectionSpace ensureSysCollectionSpace(Sequoiadb sdb) {
        if (!sdb.isCollectionSpaceExist(SYS_CS)) {
            return sdb.createCollectionSpace(SYS_CS);
        }
        else {
            return sdb.getCollectionSpace(SYS_CS);
        }
    }

    protected DBCollection createCollectionIfNotExist(CollectionSpace cs, String clName)
            throws ScmToolsException {
        DBCollection cl = SdbHelper.getCL(cs, clName);
        if (cl == null) {
            cl = SdbHelper.createCL(cs, clName);
        }
        return cl;
    }

    protected void dropCollectionIfExists(CollectionSpace cs, String clName)
            throws ScmToolsException {
        if (cs.isCollectionExist(clName)) {
            SdbHelper.dropCL(cs, clName);
        }
    }

    protected void createUniqueIdx(DBCollection cl, String idxName, String idxKey)
            throws ScmToolsException {
        SdbHelper.createIdx(cl, idxName, idxKey, true, false);
    }
}
