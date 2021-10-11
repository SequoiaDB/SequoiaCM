package com.sequoiacm.cloud.tools.common;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

public class SdbHelper {
    public final static int DEFAULT_SITE_ID = 1;

    public static Logger logger = LoggerFactory.getLogger(SdbHelper.class);

    public static class SdbErrorCode {
        public static final int SDB_IXM_DUP_KEY = -38;
        public static final int SDB_RTN_COORD_ONLY = -159;
        public static final int SDB_CAT_NO_MATCH_CATALOG = -135;
        public static final int SDB_RELINK_SUB_CL = -235;
        public static final int SDB_IXM_REDEF = -247;
    }

    public static Sequoiadb connectUrls(String urls, String userName, String passwd)
            throws ScmToolsException {
        List<String> sdblist = new ArrayList<>();
        String[] sdbarr = urls.split(",");
        for (String sdbstr : sdbarr) {
            sdblist.add(sdbstr);
        }
        Sequoiadb db = null;
        try {
            db = new Sequoiadb(sdblist, userName, passwd, null);
        } catch (Exception e) {
            logger.error("Can't connect to sequoiadb,url:" + urls, e);
            throw new ScmToolsException("Can't connect to sequoiadb,url:" + urls + ",errorMsg:"
                    + e.getMessage(), ScmExitCode.SDB_ERROR);
        }

        if (!isCoord(db)) {
            logger.error("This url is not coord,url:" + urls);
            throw new ScmToolsException("This url is not coord,url:" + urls,
                    ScmExitCode.INVALID_ARG);
        }

        try {
            db.setSessionAttr((BSONObject) JSON.parse("{PreferedInstance:'M'}"));
        } catch (Exception e) {
            closeCursorsAndConnection(db);
            logger.error("Failed to set sdb connection's attribute:{PreferedInstance:'M'}", e);
            throw new ScmToolsException(
                    "Failed to set sdb connection's attribute:{PreferedInstance:'M'},errorMsg:"
                            + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }

        return db;
    }

    private static boolean isCoord(Sequoiadb db) throws ScmToolsException {
        try {
            DBCursor cursor = db.listDomains(null, null, null, null);
            cursor.close();
            return true;
        } catch (BaseException e) {
            if (e.getErrorCode() == SdbErrorCode.SDB_RTN_COORD_ONLY) {
                return false;
            } else {
                logger.error("Failed to check sdb connection's type by listDomains", e);
                throw new ScmToolsException(
                        "Failed to check sdb connection's type by listDomains, unexpected error:"
                                + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
            }

        } catch (Exception e) {
            logger.error("Failed to check sdb connection's type by listDomains", e);
            throw new ScmToolsException(
                    "Failed to check sdb connection's type by listDomains, unexpected error:"
                            + e.getMessage(), ScmExitCode.SDB_ERROR);
        }
    }

    public static DBCollection getCLWithCheck(CollectionSpace cs, String clName)
            throws ScmToolsException {
        DBCollection cl = getCL(cs, clName);
        if (cl != null) {
            return cl;
        } else {
            logger.error("collection not exist:" + cs.getName() + "." + clName);
            throw new ScmToolsException("collection not exist:" + cs.getName() + "." + clName,
                    ScmExitCode.SCM_NOT_EXIST_ERROR);
        }
    }

    public static CollectionSpace getCSWithCheck(Sequoiadb db, String csName)
            throws ScmToolsException {
        CollectionSpace cs = getCS(db, csName);
        if (cs != null) {
            return cs;
        } else {
            logger.error("collection space not exist:" + csName);
            throw new ScmToolsException("collection space not exist:" + csName,
                    ScmExitCode.SCM_NOT_EXIST_ERROR);
        }
    }

    public static CollectionSpace getCS(Sequoiadb db, String csName) throws ScmToolsException {
        try {
            if (db.isCollectionSpaceExist(csName)) {
                return db.getCollectionSpace(csName);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("get " + csName + " collection space occur error", e);
            throw new ScmToolsException("get " + csName + " collection space occur error:"
                    + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }

    }

    public static CollectionSpace createCS(Sequoiadb db, String csName, BSONObject option)
            throws ScmToolsException {
        try {
            CollectionSpace cs = db.createCollectionSpace(csName, option);
            return cs;
        } catch (Exception e) {
            logger.error("Failed to create collection space:" + csName, e);
            throw new ScmToolsException("Failed to create collection space:" + csName + ",errorMsg:"
                    + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static CollectionSpace createCS(Sequoiadb db, String csName) throws ScmToolsException {
        try {
            CollectionSpace cs = db.createCollectionSpace(csName);
            return cs;
        } catch (Exception e) {
            logger.error("Failed to create collection space:" + csName, e);
            throw new ScmToolsException("Failed to create collection space:" + csName + ",errorMsg:"
                    + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static DBCollection getCL(CollectionSpace cs, String clName) throws ScmToolsException {
        try {
            if (cs.isCollectionExist(clName)) {
                return cs.getCollection(clName);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("get " + clName + " collection occur error", e);
            throw new ScmToolsException("get " + clName + " collection occur error:"
                    + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static DBCollection createCL(CollectionSpace cs, String clName) throws ScmToolsException {
        return createCL(cs, clName, null);
    }

    public static DBCollection createCL(CollectionSpace cs, String clName, BSONObject option)
            throws ScmToolsException {
        try {
            DBCollection cl = cs.createCollection(clName, option);
            return cl;
        } catch (Exception e) {
            logger.error("failed to create collection:" + cs.getName() + "." + clName, e);
            throw new ScmToolsException("failed to create collection:" + cs.getName() + "."
                    + clName + ",error:" + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static void dropCL(CollectionSpace cs, String clName) throws ScmToolsException {
        try {
            cs.dropCollection(clName);
        } catch (Exception e) {
            logger.error("failed to drop collection:" + cs.getName() + "." + clName, e);
            throw new ScmToolsException("failed to drop collection:" + cs.getName() + "."
                    + clName + ",error:" + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static void createIdx(DBCollection cl, String idxName, String idxKey, boolean isUnique,
                                 boolean enforce) throws ScmToolsException {
        try {
            cl.createIndex(idxName, idxKey, isUnique, enforce);
        } catch (BaseException e) {
            if (e.getErrorCode() == SdbHelper.SdbErrorCode.SDB_IXM_REDEF) {
                return;
            }
            logger.error("Failed to create " + idxName + " index on " + cl.getFullName(), e);
            throw new ScmToolsException("Failed to create " + idxName + " index on "
                    + cl.getFullName() + ",errorMsg:" + processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        } catch (Exception e) {
            logger.error("Failed to create " + idxName + " index on " + cl.getFullName(), e);
            throw new ScmToolsException("Failed to create " + idxName + " index on "
                    + cl.getFullName() + ",errorMsg:" + e.getMessage(),
                    ScmExitCode.SDB_ERROR);
        }
    }

    public static void insert(DBCollection cl, BSONObject bson) throws ScmToolsException {
        try {
            cl.insert(bson);
        } catch (BaseException e) {
            logger.error("Failed to insert record to collection,record:" + bson.toString()
                    + ",collection:" + cl.getFullName(), e);
            if (e.getErrorCode() == SdbHelper.SdbErrorCode.SDB_IXM_DUP_KEY) {
                throw new ScmToolsException(
                        "Failed to insert record to collection,record already exists,record:"
                                + bson.toString() + ",collection:" + cl.getFullName()
                                + ",error msg:" + SdbHelper.processSdbErrorMsg(e),
                        ScmExitCode.SCM_ALREADY_EXIST_ERROR);

            }
            throw new ScmToolsException("Failed to insert record to collection,record:"
                    + bson.toString() + ",collection:" + cl.getFullName() + ",error msg:"
                    + SdbHelper.processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        } catch (Exception e) {
            logger.error("Failed to insert record to collection,record:" + bson.toString()
                    + ",collection:" + cl.getFullName(), e);
            throw new ScmToolsException("Failed to insert record to collection,record:"
                    + bson.toString() + ",collection:" + cl.getFullName() + ",error msg:"
                    + e.getMessage(), ScmExitCode.SDB_ERROR);
        }
    }

    public static BSONObject queryOne(DBCollection cl, BSONObject matcher, BSONObject selector,
                                      BSONObject orderby) throws ScmToolsException {
        try {
            return cl.queryOne(matcher, selector, orderby, null, 0);
        } catch (BaseException e) {
            logger.error(
                    "failed to query on " + cl.getFullName() + ",matcher:" + matcher.toString(), e);
            throw new ScmToolsException("failed to query on " + cl.getFullName() + ",matcher:"
                    + matcher.toString() + ",errorMsg:" + processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        }
    }

    public static void update(DBCollection cl, BSONObject matcher, BSONObject modifier,
                              BSONObject hint) throws ScmToolsException {
        try {
            cl.update(matcher, modifier, hint);
        } catch (Exception e) {
            logger.error(
                    "Failed to update on " + cl.getFullName() + ",matcher:" + matcher.toString()
                            + ",modifier:" + modifier.toString(), e);
            throw new ScmToolsException("Failed to update on " + cl.getFullName() + ",matcher:"
                    + matcher.toString() + ",modifier:" + modifier.toString() + ",errorMsg:"
                    + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static DBCursor listLobs(DBCollection cl) throws ScmToolsException {
        try {
            return cl.listLobs();
        } catch (Exception e) {
            throw new ScmToolsException("Failed to list lobs,collection:" + cl.getFullName()
                    + ",errorMsg:" + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static DBCursor query(DBCollection cl, BSONObject matcher) throws ScmToolsException {
        try {
            DBCursor c = cl.query(matcher, null, null, null);
            return c;
        } catch (Exception e) {
            logger.error(
                    "Failed to query on " + cl.getFullName() + ",matcher:" + matcher.toString(), e);
            throw new ScmToolsException("Failed to query on " + cl.getFullName() + ",matcher:"
                    + matcher.toString() + ",errorMsg:" + processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        }

    }

    public static void remove(DBCollection cl, BSONObject matcher) throws ScmToolsException {
        try {
            cl.delete(matcher);
        } catch (Exception e) {
            logger.error("Failed to remove record,matcher:" + matcher.toString() + ",collection:"
                    + cl.getFullName(), e);
            throw new ScmToolsException("Failed to remove record,matcher:" + matcher.toString()
                    + "collection:" + cl.getFullName() + ",errorMsg:" + processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        }
    }

    public static void validateUrl(List<String> urlList, String username, String password)
            throws ScmToolsException {

        Sequoiadb db = null;
        try {
            db = new Sequoiadb(urlList, username, password, null);
            if (!isCoord(db)) {
                logger.error("data source url" + urlList.toString()
                        + " is invalid,errorMsg:is not coord url");
                throw new ScmToolsException("data source url" + urlList.toString()
                        + " is invalid,errorMsg:is not coord url", ScmExitCode.INVALID_ARG);
            }
        } catch (ScmToolsException e) {
            throw e;
        } catch (Exception e) {
            logger.error("data source url " + urlList.toString() + " is invalid,errorMsg:"
                    + processSdbErrorMsg(e), e);
            throw new ScmToolsException("data source url " + urlList.toString()
                    + " is invalid,errorMsg:" + processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        } finally {
            SdbHelper.closeCursorsAndConnection(db);
        }
    }

    public static void checkSdbDomainExist(String url, String user, String passwd, String domainName)
            throws ScmToolsException {
        Sequoiadb db = SdbHelper.connectUrls(url, user, passwd);
        DBCursor c;
        try {
            c = db.listDomains((BSONObject) JSON.parse("{Name:'" + domainName + "'}"), null, null,
                    null);
            if (!c.hasNext()) {
                logger.error("Domain is not exist in sequoiadb=" + url + ",domainName="
                        + domainName);
                throw new ScmToolsException("Domain is not exist in sequoiadb=" + url
                        + ",domainName=" + domainName, ScmExitCode.SCM_NOT_EXIST_ERROR);
            }
        } catch (ScmToolsException e) {
            throw e;
        } catch (Exception e) {
            logger.error("unable to determine if there is domain in sequoiadb=" + url, e);
            throw new ScmToolsException("unable to determine if there is domain in sequoiadb="
                    + url + ",erromsg:" + SdbHelper.processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        } finally {
            SdbHelper.closeCursorsAndConnection(db);
        }
    }

    public static Object getValueWithCheck(BSONObject obj, String key) throws ScmToolsException {
        Object ret = obj.get(key);
        if (ret == null) {
            throw new ScmToolsException("record missing field:" + key + ",bson:" + obj.toString(),
                    ScmExitCode.SCM_META_RECORD_ERROR);
        }
        return ret;
    }

    public static int generateCLId(Sequoiadb db, String csName, String clName)
            throws ScmToolsException {
        CollectionSpace cs = getCS(db, csName);
        if (cs == null) {
            return DEFAULT_SITE_ID;
        }
        DBCollection cl = getCL(cs, clName);
        if (cl == null) {
            return DEFAULT_SITE_ID;
        }

        // try to create a id that greater than the max id in siteCL
        BSONObject res = queryOne(cl, (BSONObject) JSON.parse("{id:{$exists:1}}"), null,
                (BSONObject) JSON.parse("{id:-1}"));
        if (res != null) {
            int currentMax = (int) res.get("id");
            if (currentMax != Integer.MAX_VALUE) {
                return currentMax + 1 > 0 ? currentMax + 1 : 1;
            }
        } else {
            return 1;
        }

        // try to create id
        int count = 1;
        while (true) {
            BSONObject queryRes = SdbHelper.queryOne(cl,
                    (BSONObject) JSON.parse("{id:" + count + "}"), null, null);

            if (queryRes == null) {
                return count;
            }
            if (count == Integer.MAX_VALUE) {
                break;
            }
            count++;
        }
        logger.error("Failed to generate id,collection:" + cl.getFullName());
        throw new ScmToolsException("Failed to generate id,collection:" + cl.getFullName(),
                ScmExitCode.SYSTEM_ERROR);
    }

    public static String processSdbErrorMsg(Exception e) {
        String notNeed = "\n Exception Detail:";
        String notNeed2 = "\r\n Exception Detail:";
        if (e.getMessage().endsWith(notNeed)) {
            return e.getMessage().substring(0, e.getMessage().length() - notNeed.length());
        } else if (e.getMessage().endsWith(notNeed2)) {
            return e.getMessage().substring(0, e.getMessage().length() - notNeed2.length());
        } else {
            return e.getMessage();
        }
    }

    public static void closeCursorsAndConnection(Sequoiadb db) {
        if (db != null) {
            try {
                db.closeAllCursors();
            } catch (Exception e) {
                logger.warn("close all cursors occur error", e);
            }
            try {
                db.disconnect();
            } catch (Exception e) {
                logger.warn("disconnect db connection occur error", e);
            }
        }
    }

    public static DBCursor listCL(Sequoiadb db) throws ScmToolsException {
        try {
            return db.listCollections();
        } catch (Exception e) {
            logger.error("failed to list cl", e);
            throw new ScmToolsException("failed to list cs,error msg:" + processSdbErrorMsg(e),
                    ScmExitCode.SDB_ERROR);
        }
    }

    public static DBCursor getList(Sequoiadb db, int type, BSONObject query, BSONObject selector,
                                   BSONObject orderBy) throws ScmToolsException {
        try {
            return db.getList(type, query, selector, orderBy);
        } catch (Exception e) {
            logger.error("failed to get list,type:" + type + "query:" + query, e);
            throw new ScmToolsException("failed to get list,type:" + type + "query:" + query
                    + "error msg:" + processSdbErrorMsg(e), ScmExitCode.SDB_ERROR);
        }
    }

    public static void attachCL(DBCollection cl, String fullName, BSONObject options)
            throws ScmToolsException {
        try {
            cl.attachCollection(fullName, options);
        } catch (BaseException e) {
            if (e.getErrorCode() != SdbErrorCode.SDB_RELINK_SUB_CL) {
                logger.error("attach cl failed:mainCL=" + cl.getFullName() + ",subCL=" + fullName,
                        e);
                throw new ScmToolsException("attach cl failed:mainCL=" + cl.getFullName()
                        + ",subCL=" + fullName + ",errorMsg:" + processSdbErrorMsg(e),
                        ScmExitCode.SDB_ERROR);
            } else {
                // not thing
                logger.warn("attach cl failed:mainCL=" + cl.getFullName() + ",subCL=" + fullName
                        + ",cause by:" + SdbErrorCode.SDB_RELINK_SUB_CL);
            }
        } catch (Exception e) {
            logger.error("attach cl failed:mainCL=" + cl.getFullName() + ",subCL=" + fullName, e);
            throw new ScmToolsException("attach cl failed:mainCL=" + cl.getFullName() + ",subCL="
                    + fullName + ",errorMsg:" + e.getMessage(), ScmExitCode.SDB_ERROR);
        }
    }
}
