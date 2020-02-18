package com.sequoiacm.cloud.tools.common;

import java.util.Properties;

import org.springframework.util.StringUtils;

import com.sequoiacm.cloud.tools.exception.ScmExitCode;
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

    protected ScmSysTableProcessor(String sdbUrl, String username, String password)
            throws ScmToolsException {
        if (!StringUtils.hasText(sdbUrl)) {
            throw new ScmToolsException("sdb url is empty or not specified",
                    ScmExitCode.INVALID_ARG);
        }
        this.sdbUrl = sdbUrl;
        this.username = username;
        this.password = password;
    }

    protected ScmSysTableProcessor(Properties properties) throws ScmToolsException {
        this.sdbUrl = properties.getProperty(FIELD_SDB_URLS);
        if (!StringUtils.hasText(this.sdbUrl)) {
            throw new ScmToolsException("sdb url is empty or not specified:key=" + FIELD_SDB_URLS,
                    ScmExitCode.INVALID_ARG);
        }
        this.username = properties.getProperty(FIELD_SDB_USERNAME);
        this.password = properties.getProperty(FIELD_SDB_PASSWORD);
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
