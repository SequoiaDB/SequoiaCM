package com.sequoiacm.cephswift.dataoperation;

import java.io.InputStream;

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
    private InputStream inputStream;
    private boolean isEof;
    private long currentPosition;

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
            inputStream = obj.downloadObjectAsInputStream();
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
    public void close() {
        try {
            inputStream.close();
        }
        catch (Exception e) {
            logger.warn("close inputStream failed:container=" + containerName + ",object="
                    + objectName, e);
        }
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws CephSwiftException {
        try {
            int readLen = inputStream.read(buff, offset, len);
            if (readLen == -1) {
                if (currentPosition != size) {
                    throw new CephSwiftException(CephSwiftException.ERR_ENTITY_IS_CORRUPTED,
                            "failed read data,data size is inconsistent:container=" + containerName
                            + ",object=" + objectName + ",expectSize=" + size
                            + ",actualSize=" + currentPosition);
                }
                this.isEof = true;
            }
            currentPosition += readLen;
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
    public void seek(long size) throws CephSwiftException {
        try {
            if (size < currentPosition) {
                throw new CephSwiftException(
                        "can not seek back,currentPosition=" + currentPosition + ",seekSize="
                                + size + ",container=" + containerName + ",object=" + objectName);
            }
            else if (size == currentPosition) {
                return;
            }

            long actualSize = inputStream.skip(size - currentPosition);

            if (actualSize == size - currentPosition) {
                currentPosition += actualSize;
            }
            else if (actualSize < size - currentPosition) {
                if (actualSize == 0 && inputStream.read(new byte[0]) == -1) {
                    throw new CephSwiftException(CephSwiftException.ERR_ENTITY_IS_CORRUPTED,
                            "skip failed,stream is at the end of file:container=" + containerName
                            + ",object=" + objectName + ",currentPosition="
                            + currentPosition + ",seekSize=" + size);
                }

                logger.debug("inputstream skip " + actualSize + "bytes,expect skip "
                        + (size - currentPosition) + " bytes,currentPosition=" + currentPosition
                        + ",seekSize=" + size + ",container=" + containerName + ",object="
                        + objectName + ",do seek again now");
                currentPosition += actualSize;
                seek(size);
            }
            else {
                throw new CephSwiftException(
                        "seek failed,expect skip " + (size - currentPosition)
                        + " bytes,actual skip " + actualSize + " bytes,container="
                        + containerName + ",object=" + objectName);
            }
        }
        catch (CephSwiftException e) {
            logger.error("seek data failed:container=" + containerName + ",object=" + objectName);
            throw e;
        }
        catch (Exception e) {
            logger.error("seek data failed:container=" + containerName + ",object=" + objectName);
            throw new CephSwiftException(
                    "seek data failed:container=" + containerName + ",object=" + objectName, e);
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
