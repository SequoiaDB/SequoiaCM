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

import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.hbase.HbaseException;
import com.sequoiacm.hbase.dataservice.HbaseDataService;

public class HbaseDataReaderImpl implements ScmDataReader {
    private static final Logger logger = LoggerFactory.getLogger(HbaseDataReaderImpl.class);
    private byte[] dataFamily = Bytes.toBytes(HbaseCommonDefine.HBASE_DATA_FAMILY);
    private byte[] metaFamily = Bytes.toBytes(HbaseCommonDefine.HBASE_META_FAMILY);

    private int siteId;
    private String tableName;
    private byte[] fileId;
    private HbaseDataService dataService;
    private Connection con;
    private Table table;
    private boolean isEof = false;
    private long fileSize = 0;

    // for check fileSize is correct
    private long haveGetPieceSize = 0;

    private long nextPieceNum = 0;

    private byte[] currentPieceArr;
    private int currentPieceArrOff;

    @SlowLog(operation = "openReader", extras = {
            @SlowLogExtra(name = "readHbaseTableName", data = "tableName"),
            @SlowLogExtra(name = "readHbaseFileId", data = "fileId") })
    public HbaseDataReaderImpl(int siteId, String tableName, String fileId,
            ScmService service) throws HbaseException {
        try {
            this.siteId = siteId;
            this.tableName = tableName;
            this.fileId = Bytes.toBytes(fileId);
            this.dataService = (HbaseDataService)service;
            //            this.con = dataService.getConnection(customConf);
            this.con = dataService.getConnection();
            this.table = dataService.getTable(con, tableName);

            // check file is available and get file size
            Get get = new Get(this.fileId);
            byte[] fileStatusQua = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_STATUS_QUALIFIER);
            byte[] fileSizeQua = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_SIZE_QUALIFIER);
            get.addColumn(metaFamily, fileStatusQua);
            get.addColumn(metaFamily, fileSizeQua);
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
            byte[] fileSize = res.getValue(metaFamily, fileSizeQua);
            String fileSizeStr = Bytes.toString(fileSize);
            this.fileSize = Long.valueOf(fileSizeStr);
        }
        catch (HbaseException e) {
            logger.error("construct HbaseFileContentReader failed:siteId=" + siteId + ",tableName="
                    + tableName + ",fileId=" + fileId);
            close();
            throw e;
        }
        catch (Exception e) {
            logger.error("construct HbaseFileContentReader failed:siteId=" + siteId + ",tableName="
                    + tableName + ",fileId=" + fileId);
            close();
            throw new HbaseException(
                    "construct HbaseFileContentReader failed:siteId=" + siteId + ",tableName="
                            + tableName + ",fileId=" + fileId, e);
        }
    }

    @Override
    @SlowLog(operation = "closeReader")
    public void close() {
        closeTable(table);
        closeConnection(con);
        dataService = null;
        fileId = null;
        dataFamily = null;
        metaFamily = null;
        currentPieceArr = null;
        table = null;
        tableName = null;
        isEof = true;
    }

    @Override
    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws HbaseException {
        if (isEof) {
            return -1;
        }

        try {
            if (currentPieceArr != null && currentPieceArrOff < currentPieceArr.length) {
                if (len <= currentPieceArr.length - currentPieceArrOff) {
                    System.arraycopy(currentPieceArr, currentPieceArrOff, buff, offset, len);
                    currentPieceArrOff += len;
                    return len;
                }
                else {
                    int firstCopyLen = currentPieceArr.length - currentPieceArrOff;
                    System.arraycopy(currentPieceArr, currentPieceArrOff, buff, offset,
                            firstCopyLen);
                    len = len - firstCopyLen;
                    offset = offset + firstCopyLen;
                    // get
                    byte[] pieceByteArr = getPiece(nextPieceNum);
                    if (pieceByteArr == null) {
                        if (haveGetPieceSize != fileSize) {
                            throw new HbaseException(HbaseException.HBASE_ERROR_FILE_CORRUPTED,
                                    "failed to read data,get piece return null,data is not exist or corrupted,:siteId="
                                            + siteId + ",tableName=" + tableName + ",fileId="
                                            + Bytes.toString(fileId) + ",pieceNum=" + nextPieceNum);
                        }
                        isEof = true;
                        nextPieceNum++;
                        return firstCopyLen;
                    }

                    // copy
                    int secondCopyLen = copyToUserBuffer(pieceByteArr, buff, offset, len);
                    nextPieceNum++;
                    haveGetPieceSize += pieceByteArr.length;
                    return firstCopyLen + secondCopyLen;
                }
            }
            else {
                byte[] pieceByteArr = getPiece(nextPieceNum);
                if (pieceByteArr == null) {
                    if (haveGetPieceSize != fileSize) {
                        throw new HbaseException(HbaseException.HBASE_ERROR_FILE_CORRUPTED,
                                "failed to read data,get piece return null,data is not exist or corrupted,:siteId="
                                        + siteId + ",tableName=" + tableName + ",fileId="
                                        + Bytes.toString(fileId) + ",pieceNum=" + nextPieceNum);
                    }
                    isEof = true;
                    nextPieceNum++;
                    return -1;
                }
                nextPieceNum++;
                haveGetPieceSize += pieceByteArr.length;
                return copyToUserBuffer(pieceByteArr, buff, offset, len);
            }
        }
        catch (HbaseException e) {
            logger.error("failed to read data:siteId=" + siteId + ",tableName=" + tableName
                    + ",fileId=" + Bytes.toString(fileId));
            throw e;
        }
        catch (Exception e) {
            logger.error("failed to read data:siteId=" + siteId + ",tableName=" + tableName
                    + ",fileId=" + Bytes.toString(fileId));
            throw new HbaseException(
                    "failed to read data:siteId=" + siteId + ",tableName=" + tableName + ",fileId="
                            + Bytes.toString(fileId), e);
        }
    }

    private int copyToUserBuffer(byte[] src, byte[] dest, int destOffset, int destLen) {
        if (src.length > destLen) {
            System.arraycopy(src, 0, dest, destOffset, destLen);
            currentPieceArr = src;
            currentPieceArrOff = destLen;
            return destLen;
        }
        else {
            System.arraycopy(src, 0, dest, destOffset, src.length);
            currentPieceArrOff = 0;
            currentPieceArr = null;
            return src.length;
        }
    }

    private byte[] getPiece(long pieceNum) throws HbaseException {
        Get get = new Get(fileId);
        byte[] currentPieceQua = Bytes.toBytes(HbaseCommonDefine.HBASE_FILE_PIECE_NUM_PREFIX
                + pieceNum);
        get.addColumn(dataFamily, currentPieceQua);
        Result res = dataService.get(table, get);
        return res.getValue(dataFamily, currentPieceQua);
    }

    @Override
    @SlowLog(operation = "seekData")
    public void seek(long size) throws HbaseException {
        try {
            if (size > fileSize) {
                throw new HbaseException(
                        "seek size great than file size:fileSize=" + fileSize + ",seekSize=" + size);
            }

            if (size == fileSize) {
                isEof = true;
                return;
            }

            // if come here,reset isEof is false,make sure read() is available
            isEof = false;

            long seekToPiece = size / HbaseCommonDefine.HBASE_FILE_PIECE_SIZE;
            int pieceOffset = (int) (size % HbaseCommonDefine.HBASE_FILE_PIECE_SIZE);
            currentPieceArr = getPiece(seekToPiece);

            if (currentPieceArr == null) {
                throw new HbaseException(HbaseException.HBASE_ERROR_FILE_CORRUPTED,
                        "failed to seek data,get piece return null,data is not exist or corrupted,:siteId="
                                + siteId + ",tableName=" + tableName + ",fileId="
                                + Bytes.toString(fileId) + ",pieceNum=" + seekToPiece);
            }
            haveGetPieceSize = currentPieceArr.length + seekToPiece
                    * HbaseCommonDefine.HBASE_FILE_PIECE_SIZE;
            currentPieceArrOff = pieceOffset;
            nextPieceNum = seekToPiece + 1;
        }
        catch (HbaseException e) {
            logger.error("failed to seek:fileId=" + Bytes.toString(fileId) + ",siteId=" + siteId
                    + ",tableName=" + tableName);
            throw e;
        }
        catch (Exception e) {
            logger.error("failed to seek:fileId=" + Bytes.toString(fileId) + ",siteId=" + siteId
                    + ",tableName=" + tableName);
            throw new HbaseException("failed to seek:fileId="
                    + Bytes.toString(fileId) + ",siteId=" + siteId + ",tableName=" + tableName, e);
        }
    }

    @Override
    public boolean isEof() {
        return isEof;
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
            logger.warn(
                    "close connection failed:siteId=" + siteId + ",fileId="
                            + Bytes.toString(fileId) + ",tableName" + tableName, e);
        }
    }

    private void closeTable(Table table) {
        try {
            if (table != null) {
                table.close();
            }
        }
        catch (Exception e) {
            logger.warn("close table failed:siteId=" + siteId + ",fileId=" + Bytes.toString(fileId)
            + ",table=" + tableName, e);
        }
    }
}
