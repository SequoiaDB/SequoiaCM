package com.sequoiacm.sequoiadb.dataservice;

import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBLob;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SequoiadbHelper {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbHelper.class);

    public static DBLob createLob(Sequoiadb sdb, String csName, String clName, String lobID)
            throws SequoiadbException {

        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            if (null == cl) {
                throw new SequoiadbException(
                        SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + clName);
            }

            return cl.createLob(new ObjectId(lobID));
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    "create lob failed:table=" + csName + "." + clName + ",id=" + lobID, e);
        }
    }

    public static void truncateLob(Sequoiadb sdb, String csName, String clName, String lobID, long length) throws SequoiadbException {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            if (null == cl) {
                throw new SequoiadbException(
                        SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + clName);
            }

            cl.truncateLob(new ObjectId(lobID), length);
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    "truncate lob failed:table=" + csName + "." + clName + ",id=" + lobID, e);
        }
    }

    public static void removeLob(Sequoiadb sdb, String csName, String clName, String lobID)
            throws SequoiadbException {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            if (null == cl) {
                throw new SequoiadbException(
                        SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + clName);
            }

            cl.removeLob(new ObjectId(lobID));
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    "remove lob failed:table=" + csName + "." + clName + ",id=" + lobID, e);
        }
    }

    public static boolean createCS(Sequoiadb sdb, String csName, BSONObject options)
            throws SequoiadbException  {
        try {
            logger.info("creating cs:csName=" + csName + ",options=" + options.toString());
            sdb.createCollectionSpace(csName, options);
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_CS_EXIST.getErrorCode()) {
                throw new SequoiadbException (e.getErrorCode(),
                        "csName=" + csName + ",options=" + options.toString(), e);
            }
            else {
                // SDB_DMS_CS_EXIST, success do nothing here.
                return false;
            }
        }
        catch (Exception e) {
            throw new SequoiadbException (SDBError.SDB_SYS.getErrorCode(),
                    "createcl failed:cs=" + csName + ",options=" + options.toString(), e);
        }
    }

    public static boolean createCL(Sequoiadb sdb, String csName, String clName, BSONObject options)
            throws SequoiadbException {
        try {
            logger.info("creating cl:clName=" + csName + "." + clName + ",options="
                    + options.toString());
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            cs.createCollection(clName, options);
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_EXIST.getErrorCode()) {
                throw new SequoiadbException(e.getErrorCode(),
                        "csName=" + csName + ",clName=" + clName + ",options=" + options.toString(),
                        e);
            }
            else {
                // SDB_DMS_EXIST, success do nothing here.
                return false;
            }
        }
        catch (Exception e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "createcl failed:cs=" + csName + ",cl=" + clName + ",options=" + options.toString(), e);
        }
    }

    public static DBLob openLob(Sequoiadb sdb, String csName, String clName, String lobID)
            throws SequoiadbException {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            if (null == cl) {
                throw new SequoiadbException(
                        SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + clName);
            }

            return cl.openLob(new ObjectId(lobID));
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    "open lob failed:table=" + csName + "." + clName + ",id=" + lobID, e);
        }
    }

    public static DBLob openLobForWrite(Sequoiadb sdb, String csName, String clName, String lobID)
            throws SequoiadbException {
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            if (null == cl) {
                throw new SequoiadbException(
                        SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + csName + "." + clName);
            }

            return cl.openLob(new ObjectId(lobID), DBLob.SDB_LOB_WRITE);
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    "open lob failed:table=" + csName + "." + clName + ",id=" + lobID, e);
        }
    }

    public static void releaseSdbResource(Sequoiadb sdb) {
        try {
            if (null != sdb) {
                sdb.releaseResource();
            }
        }
        catch (Exception e) {
            logger.warn("releaseSdbResource failed", e);
        }
    }
}
