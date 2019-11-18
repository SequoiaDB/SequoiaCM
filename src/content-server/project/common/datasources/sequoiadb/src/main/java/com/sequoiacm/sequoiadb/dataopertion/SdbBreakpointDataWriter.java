package com.sequoiacm.sequoiadb.dataopertion;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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

    private String csName;
    private String clName;
    private String lobId;
    private SdbDataLocation sdbLocation;
    private SdbDataService sds;
    private Sequoiadb sdb;
    private DBLob lob;

    private String createCsName;

    public SdbBreakpointDataWriter(SdbDataLocation sdbLocation,
            SdbDataService sds,
            String csName,
            String clName,
            String dataId,
            boolean createData)
                    throws SequoiadbException {
        this.sdbLocation = sdbLocation;
        this.sds = sds;
        this.csName = csName;
        this.clName = clName;
        this.lobId = dataId;
        this.sdb = sds.getSequoiadb();

        try {
            if (createData) {
                this.lob = createLob(lobId);
            } else {
                this.lob = openLob(lobId);
            }
        } catch (Exception e) {
            try {
                close();
            } catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
            }
            if (e instanceof SequoiadbException) {
                throw e;
            } else {
                throw new SequoiadbException(
                        SDBError.SDB_SYS.getErrorCode(),
                        "Failed to create breakpoint data writer",
                        e);
            }
        }
    }

    @Override
    public void write(long dataOffset, byte[] data, int offset, int length)
            throws SequoiadbException {
        if (dataOffset != lob.getSize()) {
            throw new SequoiadbException(
                    SDBError.SDB_SYS.getErrorCode(),
                    "Only support append data");
        }

        try {
            lob.seek(dataOffset, DBLob.SDB_LOB_SEEK_SET);
            lob.write(data, offset, length);
        } catch (BaseException e) {
            throw new SequoiadbException(
                    e.getErrorCode(),
                    "Failed to write breakpoint data",
                    e);
        }
    }

    @Override
    public void truncate(long length) throws SequoiadbException {
        if (lob.getSize() <= length) {
            return;
        }

        closeLob();
        SequoiadbHelper.truncateLob(sdb, csName, clName, lobId, length);
        this.lob = openLob(lobId);
    }

    @Override
    public void flush() throws SequoiadbException {
        closeLob();
        this.lob = openLob(lobId);
    }

    private void closeLob() throws SequoiadbException {
        try {
            if (null != lob) {
                lob.close();
                lob = null;
            }
        } catch (BaseException e) {
            throw new SequoiadbException(
                    e.getErrorCode(),
                    String.format("Failed to close lob: %s.%s/%s",
                            csName, clName, lobId),
                    e);
        } catch (Exception e) {
            throw new SequoiadbException(
                    SDBError.SDB_SYS.getErrorCode(),
                    String.format("Failed to close lob: %s.%s/%s",
                            csName, clName, lobId),
                    e);
        }
    }

    @Override
    public void close() throws SequoiadbException {
        try {
            closeLob();
        } finally {
            // release connection
            if (null != sdb) {
                sds.releaseSequoiadb(sdb);
            }
            sds = null;
            sdb = null;
        }
    }

    private DBLob createLobInner(String lobId) throws SequoiadbException {
        try {
            return SequoiadbHelper.createLob(sdb, csName, clName, lobId);
        } catch (SequoiadbException e) {
            if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()) {
                boolean isCreatedCs = SequoiadbHelper.createCS(sdb, csName, generateCSOptions());
                SequoiadbHelper.createCL(sdb, csName, clName, generateCLOptions());
                if(isCreatedCs) {
                    this.createCsName = csName;
                }
                return SequoiadbHelper.createLob(sdb, csName, clName, lobId);
            } else if (e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                /*
                 * since sdb have cached cs And cl. we should call
                 * sdb.isCollectionSpaceExist to ensure the cs is exist or not
                 */
                if (!sdb.isCollectionSpaceExist(csName)) {
                    SequoiadbHelper.createCS(sdb, csName, generateCSOptions());
                }
                SequoiadbHelper.createCL(sdb, csName, clName, generateCLOptions());
                return SequoiadbHelper.createLob(sdb, csName, clName, lobId);
            } else {
                throw e;
            }
        }
    }

    private DBLob createLob(String lobId) throws SequoiadbException {
        return createLobInner(lobId);
    }

    private DBLob openLob(String lobId) throws SequoiadbException {
        return SequoiadbHelper.openLobForWrite(
                sdb, csName, clName, lobId);
    }

    private BSONObject generateCSOptions() throws SequoiadbException {
        try {
            BSONObject options = new BasicBSONObject();
            BSONObject tmp = sdbLocation.getDataCSOptions();
            options.put("Domain", sdbLocation.getDomain());
            options.putAll(tmp);
            return options;
        }
        catch (Exception e) {
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "get sdb cl options failed:cs=" + csName +",cl=" + clName, e);
        }
    }

    private BSONObject generateCLOptions() throws SequoiadbException {
        try {
            BSONObject key = new BasicBSONObject("_id", 1);
            BSONObject options = new BasicBSONObject();
            options.put("ShardingType", "hash");
            options.put("ShardingKey", key);
            options.put("ReplSize", -1);
            options.put("AutoSplit", true);

            BSONObject tmp = sdbLocation.getDataCLOptions();
            options.putAll(tmp);

            return options;
        }
        catch (Exception e) {
            logger.error("get sdb cl options failed:cs={},cl={}", csName, clName);
            throw new SequoiadbException(SDBError.SDB_SYS.getErrorCode(),
                    "get sdb cl options failed:cs=" + csName +",cl=" + clName, e);
        }
    }

    @Override
    public String getCreatedTableName() {
        return createCsName;
    }
}
