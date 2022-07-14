package com.sequoiacm.hbase.dataoperation;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.hbase.HbaseException;
import com.sequoiacm.hbase.dataservice.HbaseDataService;

public class HbaseDataDeleterImpl implements ScmDataDeletor {
    private static final Logger logger = LoggerFactory.getLogger(HbaseDataDeleterImpl.class);

    private byte[] metaFamily = Bytes.toBytes(HbaseCommonDefine.HBASE_META_FAMILY);
    private int siteId;
    private String fileId;
    private String tableName;
    private HbaseDataService dataService;

    public HbaseDataDeleterImpl(int siteId, String fileId, String tableName,
            ScmService service) throws HbaseException {
        this.siteId = siteId;
        this.fileId = fileId;
        this.tableName = tableName;
        this.dataService = (HbaseDataService) service;
    }

    @Override
    @SlowLog(operation = "deleteData", extras = @SlowLogExtra(name = "deleteFileId", data = "fileId"))
    public void delete() throws HbaseException {
        Connection con = null;
        Table table = null;
        try {
            byte[] fileIdArr = Bytes.toBytes(fileId);

            //            con = dataService.getConnection(customConf);
            con = dataService.getConnection();
            table = dataService.getTable(con, tableName);

            // check file is available
            Get get = new Get(fileIdArr);
            byte[] fileStatusQua = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_STATUS_QUALIFIER);
            get.addColumn(metaFamily, fileStatusQua);
            Result res = dataService.get(table, get);
            if (res.isEmpty()) {
                throw new HbaseException(HbaseException.HBASE_ERROR_FILE_NOTEXIST,
                        "hbase file not exist:siteId=" + siteId + ",table=" + tableName
                        + ",fileId=" + fileId);
            }

            byte[] fileStatus = res.getValue(metaFamily, fileStatusQua);
            if (!Bytes.toString(fileStatus).equals(
                    HbaseCommonDefine.HbaseFileStatus.HBASE_FILE_STATUS_AVAILABLE)) {
                throw new HbaseException(HbaseException.HBASE_ERROR_FILE_STATUS_UNAVAILABLE,
                        "hbase file is unavailable:siteId=" + siteId + ",table=" + tableName
                        + ",fileId=" + fileId);
            }

            dataService.delete(table, fileIdArr);
        }
        catch (HbaseException e) {
            logger.error("delete file failed:siteId=" + siteId + ",tableName=" + tableName
                    + ",fileId" + fileId);
            throw e;
        }
        catch (Exception e) {
            logger.error("delete file failed:siteId=" + siteId + ",tableName=" + tableName
                    + ",fileId" + fileId);
            throw new HbaseException(
                    "delete file failed:siteId=" + siteId + ",tableName=" + tableName + ",fileId"
                            + fileId, e);
        }
        finally {
            closeTable(table);
            closeConnection(con);
        }
    }

    private void closeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        }
        catch (Exception e) {
            logger.warn("close connection failed:siteId=" + siteId + ",tableName=" + tableName
                    + ",fileId" + fileId, e);
        }
    }

    private void closeTable(Table table) {
        try {
            if (table != null) {
                table.close();
            }
        }
        catch (Exception e) {
            logger.warn("close table failed:siteId=" + siteId + ",tableName=" + tableName
                    + ",fileId" + fileId, e);
        }
    }

}
