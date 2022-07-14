package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.datasource.common.ScmInputStreamDataReader;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephS3DataReaderImpl implements ScmDataReader {

    private CephS3ConnWrapper conn;
    private String key;
    private String bucketName;
    private CephS3DataService dataService;
    private ScmInputStreamDataReader inputStreamDataReader;
    private long size;
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataReaderImpl.class);
    private S3Object obj;

    @SlowLog(operation = "openReader", extras = {
            @SlowLogExtra(name = "readCephS3BucketName", data = "bucketName"),
            @SlowLogExtra(name = "readCephS3ObjectKey", data = "key") })
    public CephS3DataReaderImpl(String bucketName, String key, ScmService service)
            throws CephS3Exception {
        this.dataService = (CephS3DataService) service;
        conn = dataService.getConn();
        if (conn == null) {
            throw new CephS3Exception(
                    "construct CephS3DataReaderImpl failed, cephs3 is down:bucketName=" + bucketName
                            + ",key=" + key);
        }

        this.bucketName = bucketName;
        this.key = key;
        GetObjectRequest getObjReq = new GetObjectRequest(bucketName, key);
        try {
            this.obj = conn.getObject(getObjReq);
        }
        catch (Exception e) {
            conn = dataService.releaseAndTryGetAnotherConn(conn);
            if (conn == null) {
                throw e;
            }
            logger.warn(
                    "read data failed, get another ceph conn to try again:bucketName={}, key={}, conn={}",
                    bucketName, key, conn.getUrl(), e);
            this.obj = conn.getObject(getObjReq);
        }

        try {
            if (obj == null) {
                throw new CephS3Exception(CephS3Exception.STATUS_NOT_FOUND,
                        CephS3Exception.ERR_CODE_NO_SUCH_KEY,
                        "object not exist: bucket=" + bucketName + ", key=" + key);
            }
            this.size = obj.getObjectMetadata().getContentLength();
            this.inputStreamDataReader = new ScmInputStreamDataReader(obj.getObjectContent());
        }
        catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    @SlowLog(operation = "closeReader")
    public void close() {
        conn.closeObj(obj);
        dataService.releaseConn(conn);
    }

    @Override
    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws CephS3Exception {
        try {
            return inputStreamDataReader.read(buff, offset, len);
        }
        catch (Exception e) {
            logger.error("failed to read data:bucketName=" + bucketName + ",key=" + key);
            throw new CephS3Exception(
                    "failed to read data:bucketName=" + bucketName + ",key=" + key, e);
        }
    }

    @Override
    @SlowLog(operation = "seekData")
    public void seek(long size) throws CephS3Exception {
        try {
            inputStreamDataReader.seek(size);
        }
        catch (Exception e) {
            logger.error("seek data failed:bucketName=" + bucketName + ",key=" + key);
            throw new CephS3Exception("seek data failed:bucketName=" + bucketName + ",key=" + key,
                    e);
        }
    }

    @Override
    public boolean isEof() {
        return inputStreamDataReader.isEof();
    }

    @Override
    public long getSize() {
        return size;
    }

}
