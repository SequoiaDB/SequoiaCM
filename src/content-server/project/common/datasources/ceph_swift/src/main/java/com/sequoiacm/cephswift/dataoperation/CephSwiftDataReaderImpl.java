package com.sequoiacm.cephswift.dataoperation;

import java.io.InputStream;

import com.sequoiacm.datasource.common.ScmInputStreamDataReader;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephswift.CephSwiftException;
import com.sequoiacm.cephswift.dataservice.CephSwiftDataService;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephSwiftDataReaderImpl implements ScmDataReader {
    private static final Logger logger = LoggerFactory.getLogger(CephSwiftDataReaderImpl.class);
    private String containerName;
    private String objectName;
    private CephSwiftDataService dataService;
    private long size = 0;
    private ScmInputStreamDataReader inputStreamDataReader;

    @SlowLog(operation = "openReader", extras = {
            @SlowLogExtra(name = "readCephSwiftContainerName", data = "containerName"),
            @SlowLogExtra(name = "readCephSwiftObjectName", data = "objectName") })
    public CephSwiftDataReaderImpl(String containerName, String objectName, ScmService service)
            throws CephSwiftException {
        try {
            this.containerName = containerName;
            this.objectName = objectName;
            this.dataService = (CephSwiftDataService)service;
            Account account = dataService.createAccount();
            Container container = dataService.getContainer(account, containerName);
            this.size = dataService.getObjectSize(container, objectName);
            StoredObject obj = dataService.getObject(container, objectName);
            inputStreamDataReader = new ScmInputStreamDataReader(obj.downloadObjectAsInputStream());
        }
        catch (CephSwiftException e) {
            logger.error("construct CephSwiftDataReaderImpl failed:container=" + containerName
                    + ",object=" + objectName);
            throw e;
        }
        catch (Exception e) {
            logger.error("construct CephSwiftDataReaderImpl failed:container=" + containerName
                    + ",object=" + objectName);
            throw new CephSwiftException(
                    "construct CephSwiftDataWriterImpl failed:container=" + containerName
                    + ",object=" + objectName, e);
        }
    }

    @Override
    @SlowLog(operation = "closeReader")
    public void close() {
        try {
            inputStreamDataReader.close();
        }
        catch (Exception e) {
            logger.warn("close inputStream failed:container=" + containerName + ",object="
                    + objectName, e);
        }
    }

    @Override
    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws CephSwiftException {
        try {
            int readLen = inputStreamDataReader.read(buff, offset, len);
            if (readLen == -1) {
                if (inputStreamDataReader.getCurrentPosition() != size) {
                    throw new CephSwiftException(CephSwiftException.ERR_ENTITY_IS_CORRUPTED,
                            "failed read data,data size is inconsistent:container=" + containerName
                            + ",object=" + objectName + ",expectSize=" + size
                                    + ",actualSize=" + inputStreamDataReader.getCurrentPosition());
                }
            }
            return readLen;
        }
        catch (CephSwiftException e) {
            logger.error("failed read data:container=" + containerName + ",object=" + objectName);
            throw e;
        }
        catch (Exception e) {
            logger.error("failed read data:container=" + containerName + ",object=" + objectName);
            throw new CephSwiftException(
                    "failed read data:container=" + containerName + ",object=" + objectName, e);
        }
    }

    @Override
    @SlowLog(operation = "seekData")
    public void seek(long size) throws CephSwiftException {
        try {
            inputStreamDataReader.seek(size);
        }
        catch (Exception e) {
            logger.error("seek data failed:container=" + containerName + ",object=" + objectName);
            throw new CephSwiftException(
                    "seek data failed:container=" + containerName + ",object=" + objectName, e);
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
