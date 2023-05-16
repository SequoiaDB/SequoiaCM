package com.sequoiacm.deploy.common;

import java.util.Arrays;
import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SdbTools {
    private static final Logger logger = LoggerFactory.getLogger(SdbTools.class);

    public static void dorpCS(Sequoiadb db, String name) {
        try {
            logger.debug("drop cs:sdb={}:{}, cs={}", db.getHost(), db.getPort(), name);
            db.dropCollectionSpace(name);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()) {
                throw e;
            }
        }
    }

    public static boolean isDomainExist(String urls, String user, String passwd, String domain) {
        Sequoiadb db = null;
        try {
            db = SdbTools.createSdb(urls, user, passwd);
            return db.isDomainExist(domain);
        }
        finally {
            CommonUtils.closeResource(db);
        }
    }

    public static Sequoiadb createSdb(String urls, String user, String passwd) {
        List<String> url = Arrays.asList(urls.split(","));
        return new Sequoiadb(url, user, passwd, new ConfigOptions());
    }

    public static CollectionSpace createCS(Sequoiadb db, String name, BSONObject options) {
        try {
            logger.debug("create cs:sdb={}:{}, cs={}, options:{}", db.getHost(), db.getPort(), name,
                    options);
            return db.createCollectionSpace(name, options);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_CS_EXIST.getErrorCode()) {
                return db.getCollectionSpace(name);
            }
            throw e;
        }
    }

    public static DBCollection createCL(Sequoiadb db, String csName, String clName,
            BSONObject options) {
        CollectionSpace cs = db.getCollectionSpace(csName);
        try {
            logger.debug("create cl:sdb={}{}, cl={}:{}, options", db.getHost(), db.getPort(),
                    csName, clName, options);
            return cs.createCollection(clName, options);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()) {
                return cs.getCollection(clName);
            }
            throw e;
        }
    }

    public static void createIdx(DBCollection cl, String name, BSONObject key, boolean isUnique,
            boolean enforced) {
        try {
            logger.debug("create idx:sdb={}:{}, cl={}, key={}, isUnique:{}, enfored:{}",
                    cl.getSequoiadb().getHost(), cl.getSequoiadb().getPort(), cl.getFullName(), key,
                    isUnique, enforced);
            cl.createIndex(name, key, isUnique, enforced);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode()) {
                throw e;
            }
        }
    }
}
