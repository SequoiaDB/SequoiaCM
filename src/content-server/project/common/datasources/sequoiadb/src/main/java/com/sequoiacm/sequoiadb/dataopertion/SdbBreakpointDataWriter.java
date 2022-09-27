package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiacm.sequoiadb.dataservice.SequoiadbHelper;
import com.sequoiadb.base.DBLob;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SdbBreakpointDataWriter implements ScmBreakpointDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(SdbBreakpointDataWriter.class);
    private long writeOffset;

    private String csName;
    private String clName;
    private String lobId;
    private SdbDataLocation sdbLocation;
    private SdbDataService sds;
    private Sequoiadb sdb;
    private DBLob lob;
    private ScmLockManager lockManager;
    private String createCsName;
    private MetaDataOperator metaDataOperator;
    private String siteName;

    @SlowLog(operation = "openWriter", extras = { @SlowLogExtra(name = "writeCs", data = "csName"),
            @SlowLogExtra(name = "writeCl", data = "clName"),
            @SlowLogExtra(name = "writeLobId", data = "dataId") })
    public SdbBreakpointDataWriter(SdbDataLocation sdbLocation, SdbDataService sds,
            MetaSource metaSource, String csName, String clName, String wsName, String dataId,
            boolean createData, long writeOffset, ScmLockManager lockManager)
            throws ScmDatasourceException {
        this.sdbLocation = sdbLocation;
        this.sds = sds;
        this.csName = csName;
        this.clName = clName;
        this.lobId = dataId;
        this.sdb = sds.getSequoiadb();
        this.writeOffset = writeOffset;
        this.siteName = sdbLocation.getSiteName();
        this.metaDataOperator = new MetaDataOperator(metaSource, wsName, siteName,
                sdbLocation.getSiteId());
        this.lockManager = lockManager;
        try {
            if (createData) {
                this.lob = createLob(lobId);
            }
            else {
                this.lob = openLob(lobId);
            }
        }
        catch (Exception e) {
            try {
                close();
            }
            catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
            }
            if (e instanceof SequoiadbException) {
                throw e;
            }
            else {
                throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                        "Failed to create breakpoint data writer", e);
            }
        }
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] data, int offset, int length) throws SequoiadbException {
        if (writeOffset != lob.getSize()) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "Only support append data");
        }
        seekWrite(data, offset, length);
    }

    protected void seekWrite(byte[] data, int offset, int length) throws SequoiadbException {
        try {
            lob.lockAndSeek(writeOffset, length);
            lob.write(data, offset, length);
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(), "Failed to write lob", e);
        }
        writeOffset += length;
    }

    @Override
    @SlowLog(operation = "truncateData")
    public void truncate(long length) throws SequoiadbException {
        if (lob.getSize() <= length) {
            return;
        }

        closeLob();
        SequoiadbHelper.truncateLob(sdb, csName, clName, lobId, length);
        this.lob = openLob(lobId);
    }

    @Override
    @SlowLog(operation = "flushData")
    public void flush() throws SequoiadbException {
        closeLob();
        this.lob = openLob(lobId);
    }

    @Override
    @SlowLog(operation = "completeData")
    public void complete() throws ScmDatasourceException {
        close();
    }

    @Override
    @SlowLog(operation = "abortData")
    public void abort() throws ScmDatasourceException {
        closeLob();
        SequoiadbHelper.removeLob(sdb, csName, clName, lobId);
    }

    private void closeLob() throws SequoiadbException {
        try {
            if (null != lob) {
                lob.close();
                lob = null;
            }
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(),
                    String.format("Failed to close lob: %s.%s/%s", csName, clName, lobId), e);
        }
        catch (Exception e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    String.format("Failed to close lob: %s.%s/%s", csName, clName, lobId), e);
        }
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws SequoiadbException {
        try {
            closeLob();
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

    private DBLob createLobInner(String lobId) throws ScmDatasourceException {
        try {
            return SequoiadbHelper.createLob(sdb, csName, clName, lobId);
        }
        catch (SequoiadbException e) {
            if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                boolean isCreatedCs = SdbCsRecycleHelper.createCs(sdb, csName, siteName,
                        sdbLocation, lockManager, metaDataOperator);
                if (isCreatedCs) {
                    this.createCsName = csName;
                }
                SequoiadbHelper.createCL(sdb, csName, clName, sdbLocation);
                return SequoiadbHelper.createLob(sdb, csName, clName, lobId);
            }
            else {
                throw e;
            }
        }
    }

    private DBLob createLob(String lobId) throws ScmDatasourceException {
        return createLobInner(lobId);
    }

    private DBLob openLob(String lobId) throws SequoiadbException {
        try {
            return SequoiadbHelper.openLobForWrite(sdb, csName, clName, lobId);
        }
        catch (SequoiadbException e) {
            if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                boolean recovered = SdbCsRecycleHelper.recoverIfNeeded(sdb, csName, siteName,
                        metaDataOperator, lockManager);
                if (recovered) {
                    return openLob(lobId);
                }
            }
            throw e;
        }
    }

    @Override
    public String getCreatedTableName() {
        return createCsName;
    }

    @Override
    public BSONObject getContext() {
        return null;
    }

    protected void seek(long size) throws ScmDatasourceException {
        writeOffset = size;
    }
}
