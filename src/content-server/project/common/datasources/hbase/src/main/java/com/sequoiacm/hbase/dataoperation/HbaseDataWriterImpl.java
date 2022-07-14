package com.sequoiacm.hbase.dataoperation;

import java.nio.ByteBuffer;

import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.hbase.HbaseException;
import com.sequoiacm.hbase.dataservice.HbaseDataService;

public class HbaseDataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(HbaseDataWriterImpl.class);

    private byte[] dataFamily = Bytes.toBytes(HbaseCommonDefine.HBASE_DATA_FAMILY);
    private byte[] metaFamily = Bytes.toBytes(HbaseCommonDefine.HBASE_META_FAMILY);

    private String tableName;
    private byte[] fileId;
    private HbaseDataService dataService;
    private Connection con;

    private Table table;

    byte[] buffer = null;
    private int bufferOff = 0;
    private long filePiece = 0;

    private long fileSize = 0;

    private String createdTableName;

    private ScmPoolWrapper poolWrapper;

    @SlowLog(operation = "openWriter", extras = {
            @SlowLogExtra(name = "writeHbaseTableName", data = "tableName"),
            @SlowLogExtra(name = "writeHbaseFileId", data = "fileId") })
    public HbaseDataWriterImpl(String tableName, String fileId, ScmService service)
            throws HbaseException {
        this.tableName = tableName;
        try {
            this.fileId = Bytes.toBytes(fileId);
            this.dataService = (HbaseDataService) service;
            // this.con = dataService.getConnection(customConf);
            this.con = dataService.getConnection();
            this.poolWrapper = ScmPoolWrapper.getInstance();
            this.buffer = this.poolWrapper.getBytes(HbaseCommonDefine.HBASE_FILE_PIECE_SIZE);
            createFile();
        }
        catch (HbaseException e) {
            logger.error("construct HbaseFileContentWriter failed:tableName=" + tableName
                    + ",fileId=" + fileId);
            releaseReource();
            throw e;
        }
        catch (Exception e) {
            logger.error("construct HbaseFileContentWriter failed:tableName=" + tableName
                    + ",fileId=" + fileId);
            releaseReource();
            throw new HbaseException("construct HbaseFileContentWriter failed:tableName="
                    + tableName + ",fileId=" + fileId, e);
        }
    }

    private void createFile() throws HbaseException {
        byte[] fileStatusQualifier = null;
        byte[] fileStatus = null;
        Put put = null;
        boolean isSuccess = false;
        try {
            fileStatusQualifier = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_STATUS_QUALIFIER);
            fileStatus = Bytes
                    .toBytes(HbaseCommonDefine.HbaseFileStatus.HBASE_FILE_STATUS_UNAVAILABLE);
            put = new Put(this.fileId);
            put.addColumn(metaFamily, fileStatusQualifier, fileStatus);
            table = dataService.getTable(con, tableName);
            // try to create a new row,and set file status to unavailable
            isSuccess = dataService.checkAndPut(table, this.fileId, metaFamily, fileStatusQualifier,
                    null, put);
        }
        catch (HbaseException e) {
            if (e.getErrorCode().equals(HbaseException.HBASE_ERROR_TABLE_NOTEXIST)) {
                boolean hasCreatedTable = dataService.createTable(con, tableName);
                if (hasCreatedTable) {
                    this.createdTableName = tableName;
                }
                table = dataService.getTable(con, tableName);
                isSuccess = dataService.checkAndPut(table, this.fileId, metaFamily,
                        fileStatusQualifier, null, put);
            }
            else {
                throw e;
            }
        }

        if (!isSuccess) {
            throw new HbaseException(HbaseException.HBASE_ERROR_FILE_EXIST,
                    "file data exist,table=" + tableName + ",fileId=" + Bytes.toString(fileId));
        }
    }

    @Override
    public void write(byte[] content) throws HbaseException {
        write(content, 0, content.length);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] content, int offset, int len) throws HbaseException {
        try {
            fileSize += len;
            while (len >= buffer.length - bufferOff) {
                int writeSize = buffer.length - bufferOff;
                System.arraycopy(content, offset, buffer, bufferOff, writeSize);

                sendAndClearBuffer();

                len -= writeSize;
                offset += writeSize;
            }

            System.arraycopy(content, offset, buffer, bufferOff, len);
            bufferOff += len;
        }
        catch (HbaseException e) {
            throw e;
        }
        catch (Exception e) {
            throw new HbaseException("write data failed:tableName=" + tableName + ",fileId="
                    + Bytes.toString(fileId), e);
        }
    }

    private void sendAndClearBuffer() throws HbaseException {
        Put put = new Put(fileId);
        byte[] pieceNum = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_PIECE_NUM_PREFIX + filePiece);
        put.addColumn(dataFamily, pieceNum, buffer);

        try {
            dataService.put(table, put);
        }
        catch (Exception e) {
            logger.error("put content failed,tableName=" + tableName + ",fileId="
                    + Bytes.toString(fileId));
            throw new HbaseException("put content failed,tableName=" + tableName + ",fileId="
                    + Bytes.toString(fileId), e);
        }

        filePiece++;
        bufferOff = 0;
    }

    @Override
    @SlowLog(operation = "cancelWriter")
    public void cancel() {
        try {
            dataService.delete(table, fileId);
        }
        catch (Exception e) {
            logger.warn(
                    "cancel failed:tableName=" + tableName + ",fileId=" + Bytes.toString(fileId),
                    e);
        }
        releaseReource();
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws HbaseException {
        try {
            Put put = new Put(fileId);
            if (bufferOff > 0) {
                // add buffer data
                ByteBuffer qualifierBuf = ByteBuffer.wrap(
                        Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_PIECE_NUM_PREFIX + filePiece));
                ByteBuffer valueBuf = ByteBuffer.wrap(buffer, 0, bufferOff);
                put.addColumn(dataFamily, qualifierBuf, HConstants.LATEST_TIMESTAMP, valueBuf);
                filePiece++;
                // no need
                // fileSize += bufferOff;
            }

            // change file status to available
            byte[] fileStatusQualifier = Bytes
                    .toBytes(HbaseCommonDefine.HBASE_FILE_STATUS_QUALIFIER);
            byte[] fileStatus = Bytes
                    .toBytes(HbaseCommonDefine.HbaseFileStatus.HBASE_FILE_STATUS_AVAILABLE);
            put.addColumn(metaFamily, fileStatusQualifier, fileStatus);

            // set file size
            byte[] fileSizeQualifier = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_SIZE_QUALIFIER);
            byte[] fileSizeByte = Bytes.toBytes(fileSize + "");
            put.addColumn(metaFamily, fileSizeQualifier, fileSizeByte);

            dataService.put(table, put);
        }
        catch (Exception e) {
            logger.error("close failed:table=" + tableName + ",fileId=" + Bytes.toString(fileId));
            fileSize = 0;
            throw new HbaseException(
                    "close failed:table=" + tableName + ",fileId=" + Bytes.toString(fileId), e);
        }
        finally {
            releaseReource();
        }
    }

    private void releaseReource() {
        closeTable(table);
        closeConnection(con);

        dataService = null;
        table = null;
        if (buffer != null) {
            poolWrapper.releaseBytes(buffer);
            buffer = null;
        }
        metaFamily = null;
        dataFamily = null;
        fileId = null;
        con = null;
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    private void closeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        }
        catch (Exception e) {
            logger.warn("close connection failed:table=" + tableName + ",fileId="
                    + Bytes.toString(fileId), e);
        }
    }

    private void closeTable(Table table) {
        try {
            if (table != null) {
                table.close();
            }
        }
        catch (Exception e) {
            logger.warn(
                    "close table failed:table=" + tableName + ",fileId=" + Bytes.toString(fileId),
                    e);
        }
    }

    @Override
    public String getCreatedTableName() {
        return createdTableName;
    }

}
