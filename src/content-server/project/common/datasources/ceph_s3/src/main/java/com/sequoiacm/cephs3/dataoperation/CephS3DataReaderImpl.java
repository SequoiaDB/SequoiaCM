package com.sequoiacm.cephs3.dataoperation;

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
    private S3ObjectInputStream objIs;
    private boolean isEof;
    private long size;
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataReaderImpl.class);
    private long currentPosition = 0;
    private S3Object obj;

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
            this.objIs = obj.getObjectContent();
        }
        catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        conn.closeObj(obj);
        dataService.releaseConn(conn);
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws CephS3Exception {
        try {
            int readLen = objIs.read(buff, offset, len);
            if (readLen == -1) {
                this.isEof = true;
            }
            currentPosition += readLen;
            return readLen;
        }
        catch (Exception e) {
            logger.error("failed to read data:bucketName=" + bucketName + ",key=" + key);
            throw new CephS3Exception(
                    "failed to read data:bucketName=" + bucketName + ",key=" + key, e);
        }
    }

    @Override
    public void seek(long size) throws CephS3Exception {
        if (size < currentPosition) {
            throw new CephS3Exception("can not seek back,currentPosition=" + currentPosition
                    + ",seekSize=" + size + ",bucketName=" + bucketName + ",key=" + key);
        }
        else if (size == currentPosition) {
            return;
        }
        try {
            long actualSize = objIs.skip(size - currentPosition);

            if (actualSize == size - currentPosition) {
                currentPosition += actualSize;
            }
            else if (actualSize < size - currentPosition) {
                logger.debug("S3ObjectInputStream skip " + actualSize + "bytes,expect skip "
                        + (size - currentPosition) + " bytes,currentPosition=" + currentPosition
                        + ",seekSize=" + size + ",bucketName=" + bucketName + ",key=" + key
                        + ",do seek again now");
                currentPosition += actualSize;
                seek(size);
            }
            else {
                throw new CephS3Exception("seek failed,expect skip " + (size - currentPosition)
                        + " bytes,actual skip " + actualSize + " bytes,bucketName=" + bucketName
                        + ",key=" + key);
            }
        }
        catch (CephS3Exception e) {
            logger.error("seek data failed:bucketName=" + bucketName + ",key=" + key);
            throw e;
        }
        catch (Exception e) {
            logger.error("seek data failed:bucketName=" + bucketName + ",key=" + key);
            throw new CephS3Exception("seek data failed:bucketName=" + bucketName + ",key=" + key,
                    e);
        }
    }

    @Override
    public boolean isEof() {
        return isEof;
    }

    @Override
    public long getSize() {
        return size;
    }

}
