package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
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

public class SdbLobWriter implements SdbWriter {
    private static final Logger logger = LoggerFactory.getLogger(SdbLobWriter.class);
    private int siteId;
    private String csName;
    private String clName;
    private String lobId;

    private SdbDataLocation sdbLocation = null;
    private SdbDataService sds = null;
    private Sequoiadb sdb = null;
    private DBLob lob = null;
    private long offset = 0;
    private String createdCsName;
    private ScmLockManager lockManager;
    private MetaDataOperator metaDataOperator;
    private String siteName;

    public SdbLobWriter(int siteId, ScmLocation location, ScmService service, MetaSource metaSource,
            String csName, String clName, String wsName, String dataId, ScmLockManager lockManager)
            throws SequoiadbException {
        this.siteId = siteId;
        this.csName = csName;
        this.clName = clName;
        this.lobId = dataId;
        this.siteName = location.getSiteName();
        this.metaDataOperator = new MetaDataOperator(metaSource, wsName, location.getSiteName(),
                siteId);
        this.lockManager = lockManager;
        try {
            sdbLocation = (SdbDataLocation) location;
            sds = (SdbDataService) service;
            sdb = sds.getSequoiadb();
            lob = createLob(wsName, location, service, metaSource);
        }
        catch (SequoiadbException e) {
            cancel();
            throw e;
        }
        catch (Exception e) {
            cancel();
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "create lob failed:siteId=" + siteId + ",csName=" + csName + ",clName=" + clName
                            + ",lob=" + dataId,
                    e);
        }
    }

    @Override
    public void write(byte[] data, int off, int len) throws SequoiadbException {
        try {
            lob.write(data, off, len);
            this.offset += len;
        }
        catch (BaseException e) {
            logger.error("write lob failed:siteId=" + siteId + ",cs=" + csName + ",cl=" + clName
                    + ",lobId=" + lobId);
            throw new SequoiadbException(e.getErrorCode(), "write lob failed:siteId=" + siteId
                    + ",cs=" + csName + ",cl=" + clName + ",lobId=" + lobId, e);

        }
        catch (Exception e) {
            logger.error("write lob failed:siteId=" + siteId + ",cs=" + csName + ",cl=" + clName
                    + ",lobId=" + lobId);
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(), "write lob failed:siteId="
                    + siteId + ",cs=" + csName + ",cl=" + clName + ",lobId=" + lobId, e);
        }
    }

    @Override
    public void cancel() {
        // interrupt Sequoiadb connection
        if (null != sds) {
            SequoiadbHelper.releaseSdbResource(sdb);
            sds.releaseSequoiadb(sdb);
        }
        sds = null;
        sdb = null;
    }

    @Override
    public String getCreatedTableName() {
        return createdCsName;
    }

    @Override
    public void close() throws SequoiadbException {
        try {
            if (null != lob) {
                lob.close();
                lob = null;
            }
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(), "close lob failed:siteId=" + siteId
                    + ",csName=" + csName + ",clName=" + clName + ",lobId=" + lobId, e);
        }
        catch (Exception e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(), "close lob failed:siteId="
                    + siteId + ",csName=" + csName + ",clName=" + clName + ",lobId=" + lobId, e);
        }
        finally {
            // release connection
            if (null != sdb) {
                sds.releaseSequoiadb(sdb);
            }
            sds = null;
            sdb = null;
        }
    }

    @Override
    public long getFileSize() {
        return offset;
    }

    private DBLob createLob(String wsName, ScmLocation location, ScmService service,
            MetaSource metaSource) throws SequoiadbException {
        DBLob tmpLob = null;
        try {
            tmpLob = SequoiadbHelper.createLob(sdb, csName, clName, lobId);
        }
        catch (SequoiadbException e) {
            if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                boolean isCreated = SdbCsRecycleHelper.createCs(sdb, csName, siteName, sdbLocation,
                        lockManager, metaDataOperator);
                if (isCreated) {
                    this.createdCsName = csName;
                }
                SequoiadbHelper.createCL(sdb, csName, clName, sdbLocation);
                tmpLob = SequoiadbHelper.createLob(sdb, csName, clName, lobId);
            }
            else {
                logger.error("create lob failed:siteId=" + siteId + ",cs=" + csName + ",cl="
                        + clName + ",lobId=" + lobId);
                throw e;
            }
        }
        catch (Exception e) {
            logger.error("create lob failed:siteId=" + siteId + ",cs=" + csName + ",cl=" + clName
                    + ",lobId=" + lobId);
            throw e;
        }

        return tmpLob;
    }
}
