package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiacm.sequoiadb.dataservice.SequoiadbHelper;
import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
import com.sequoiadb.base.DBLob;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdbDataReaderImpl implements ScmDataReader {
    private SdbFileContent content = null;

    @SlowLog(operation = "openReader", extras = { @SlowLogExtra(name = "readCs", data = "csName"),
            @SlowLogExtra(name = "readCl", data = "clName"),
            @SlowLogExtra(name = "readLobId", data = "id") })
    SdbDataReaderImpl(int siteId, String siteName, String csName, String clName, String wsName,
            int type, String id, ScmService service, MetaSource metaSource,
            ScmLockManager lockManager) throws SequoiadbException {
        content = new SdbFileContentLob(siteId, siteName, csName, clName, wsName, id, service,
                metaSource, lockManager);
    }

    @Override
    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws SequoiadbException {
        return content.read(buff, offset, len);
    }

    @Override
    @SlowLog(operation = "seekData")
    public void seek(long size) throws SequoiadbException {
        content.seek(size);
    }

    @Override
    @SlowLog(operation = "closeReader")
    public void close() {
        content.close();
        content = null;
    }

    @Override
    public boolean isEof() {
        return content.isEof();
    }

    @Override
    public long getSize() {
        return content.getSize();
    }
}

interface SdbFileContent {
    public int read(byte[] buff, int offset, int len) throws SequoiadbException;

    public void seek(long size) throws SequoiadbException;

    public void close();

    public boolean isEof();

    public long getSize();
}

//class SdbFileContentBson implements SdbFileContent {
//    private static final Logger logger = LoggerFactory.getLogger(SdbFileContentBson.class);
//    private Sequoiadb sdb = null;
//    private byte[] content = null;
//    private int offset = 0;
//    private int siteId = 0;
//    private SdbDataService service = null;
//
//    public SdbFileContentBson(int siteId, String csName, String clName, String objectID,
//            ScmService service) throws ScmInnerException {
//        try {
//            this.siteId = siteId;
//            this.service = (SdbDataService)service;
//            // read lob from master, in case slave node is unavailable
//            this.sdb = service.getSequoiadb();
//            BSONObject matcher = new BasicBSONObject();
//            matcher.put(FieldName.FIELD_ALL_OBJECTID, new ObjectId(objectID));
//            BSONObject record = SequoiadbHelper.queryOne(sdb, csName, clName, matcher);
//            if (null == record) {
//                throw new ScmInnerException(ScmErrorCode.SCM_DATA_NOTEXIST,
//                        "record is unexist:objectID=" + objectID);
//            }
//
//            sds.releaseSequoiadb(sdb);
//            sdb = null;
//
//            content = BsonAssist.getInstance().bsonObjectToByteArray(record);
//        }
//        catch (ScmInnerException e) {
//            close();
//            logger.error("construct ScmFileContentBson failed:siteId=" + siteId + ",csName="
//                    + csName + ",clName=" + clName + ",lobID=" + objectID);
//            if (e.getErrorCode() == ScmErrorCode.SCM_DATA_NOTEXIST) {
//                throw e;
//            }
//            else {
//                throw new ScmInnerException(ScmErrorCode.SCM_DATA_READ_ERROR,
//                        "construct ScmFileContentBson failed:siteId=" + siteId + ",csName="
//                                + csName + ",clName=" + clName + ",lobId=" + objectID, e);
//            }
//        }
//        catch (Exception e) {
//            close();
//            logger.error("construct ScmFileContentBson failed:siteId=" + siteId + ",csName="
//                    + csName + ",clName=" + clName + ",lobID=" + objectID);
//            throw new ScmInnerException(ScmErrorCode.SCM_DATA_READ_ERROR,
//                    "construct ScmFileContentBson failed:siteId=" + siteId + ",csName=" + csName
//                    + ",clName=" + clName + ",lobId=" + objectID, e);
//        }
//    }
//
//    @Override
//    public int read(byte[] buff, int offset, int len) {
//        if (offset >= content.length) {
//            return -1;
//        }
//
//        int readLen = len <= content.length - offset ? len : content.length - offset;
//        System.arraycopy(content, this.offset, buff, offset, readLen);
//        this.offset += readLen;
//
//        return readLen;
//    }
//
//    @Override
//    public void seek(long size) {
//        if (size < 0 || size > content.length) {
//            return;
//        }
//
//        offset = (int) size;
//    }
//
//    @Override
//    public void close() {
//        if (null != sds) {
//            sds.releaseSequoiadb(sdb);
//        }
//        sds = null;
//        sdb = null;
//    }
//
//    @Override
//    public boolean isEof() {
//        if (offset >= content.length) {
//            return true;
//        }
//
//        return false;
//    }
//
//    @Override
//    public long getSize() {
//        return content.length;
//    }
//}

class SdbFileContentLob implements SdbFileContent {
    private static final Logger logger = LoggerFactory.getLogger(SdbFileContentLob.class);
    private Sequoiadb sdb = null;
    private String lobId = null;
    private DBLob lob = null;
    private byte[] eofBuff = new byte[1];
    private int siteId = 0;
    private SdbDataService service = null;
    private MetaDataOperator metaDataOperator;
    private ScmLockManager lockManager;
    private String siteName;

    public SdbFileContentLob(int siteId, String siteName, String csName, String clName,
            String wsName, String lobID, ScmService service, MetaSource metaSource,
            ScmLockManager lockManager) throws SequoiadbException {
        try {

            this.lobId = lobID;
            this.siteId = siteId;
            this.service = (SdbDataService)service;
            // read lob from master, in case slave node is unavailable
            this.sdb = this.service.getSequoiadb();
            this.lockManager = lockManager;
            this.siteName = siteName;
            this.metaDataOperator = new MetaDataOperator(metaSource, wsName, siteName, siteId);
            lob = openLob(sdb, csName, clName, lobID);
        }
        catch (SequoiadbException e) {
            close();
            throw e;
        }
        catch (Exception e) {
            close();
            logger.error("open lob failed:siteId=" + siteId + ",csName=" + csName + ",clName="
                    + clName + ",lobId=" + lobID);
            throw new SequoiadbException("open lob failed:siteId="
                    + siteId + ",csName=" + csName + ",clName=" + clName + ",lobId=" + lobID, e);
        }
    }

    private DBLob openLob(Sequoiadb sdb, String csName, String clName, String lobID)
            throws SequoiadbException {
        try {
            return SequoiadbHelper.openLob(sdb, csName, clName, lobID);
        }
        catch (SequoiadbException e) {
            if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                boolean recovered = SdbCsRecycleHelper.recoverIfNeeded(sdb, siteName, csName,
                        metaDataOperator, lockManager);
                if (recovered) {
                    return openLob(sdb, csName, clName, lobID);
                }
            }
            throw e;
        }
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws SequoiadbException {
        try {
            return lob.read(buff, offset, len);
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    "read lob failed:siteId=" + siteId + ",lobId=" + lobId + ",offset=" + offset
                    + ",len=" + len, e);
        }
        catch (Exception e) {
            throw new SequoiadbException("read lob failed:siteId="
                    + siteId + ",lobId=" + lobId + ",offset=" + offset + ",len=" + len, e);
        }
    }

    @Override
    public void close() {
        try {
            if (null != lob) {
                lob.close();
                lob = null;
            }
        }
        catch (Exception e) {
            logger.warn("close lob failed:lobID=" + lobId + ",error=" + e);
        }
        finally {
            if (null != service) {
                service.releaseSequoiadb(sdb);
            }
            service = null;
            sdb = null;
        }
    }

    @Override
    public void seek(long size) throws SequoiadbException {
        try {
            lob.seek(size, DBLob.SDB_LOB_SEEK_SET);
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(), "seek lob failed:siteId=" + siteId
                    + ",lobid=" + lobId + ",size=" + size, e);
        }
        catch (Exception e) {
            throw new SequoiadbException(
                    "seek lob failed:siteId=" + siteId + ",lobid=" + lobId + ",size=" + size, e);
        }
    }

    @Override
    public boolean isEof() {
        int len = 0;
        try {
            len = lob.read(eofBuff, 0, 0);
        }
        catch (Exception e) {
            logger.warn("read lob failed:siteId=" + siteId + ",lobId=" + lobId + ",offset=0"
                    + ",len=0", e);
        }

        if (-1 == len) {
            return true;
        }

        return false;
    }

    @Override
    public long getSize() {
        return lob.getSize();
    }
}