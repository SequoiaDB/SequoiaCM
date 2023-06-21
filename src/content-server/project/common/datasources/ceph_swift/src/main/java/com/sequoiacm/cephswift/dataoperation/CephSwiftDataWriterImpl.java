package com.sequoiacm.cephswift.dataoperation;

import java.io.ByteArrayInputStream;

import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.javaswift.joss.headers.object.ObjectManifest;
import org.javaswift.joss.headers.object.ObjectMetadata;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephswift.CephSwiftException;
import com.sequoiacm.cephswift.dataservice.CephSwiftDataService;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephSwiftDataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(CephSwiftDataWriterImpl.class);
    private String containerName;
    private String objectName;
    private CephSwiftDataService dataService;
    private Container container;

    private byte[] buffer = null;

    private int bufferOff = 0;
    private int fileSize = 0;
    private int partNum = 1;
    private Account account;
    private String createdContainerName;
    private ScmPoolWrapper poolWrapper;

    @SlowLog(operation = "openWriter", extras = {
            @SlowLogExtra(name = "writeCephSwiftContainerName", data = "containerName"),
            @SlowLogExtra(name = "writeCephSwiftObjectName", data = "objectName") })
    public CephSwiftDataWriterImpl(String containerName, String objectName, ScmService service)
            throws CephSwiftException {
        try {
            this.containerName = containerName;
            this.objectName = objectName;
            this.dataService = (CephSwiftDataService) service;
            this.account = dataService.createAccount();
            this.container = dataService.getContainer(account, containerName);
            if (dataService.isObjectExist(dataService.getObject(container, objectName))) {
                throw new CephSwiftException(CephSwiftException.ERR_ENTITY_ALREADY_EXISTS,
                        "file data exists:containerName=" + containerName + ",objectName="
                                + objectName);
            }
            this.poolWrapper = ScmPoolWrapper.getInstance();
            this.buffer = poolWrapper.getBytes(CephSwiftCommonDefine.SEGMENT_OBJ_SIZE);
        }
        catch (CephSwiftException e) {
            logger.error("construct CephSwiftDataWriterImpl failed:container=" + containerName
                    + ",object=" + objectName);
            releaseResource();
            throw e;
        }
        catch (Exception e) {
            logger.error("construct CephSwiftDataWriterImpl failed:container=" + containerName
                    + ",object=" + objectName);
            releaseResource();
            throw new CephSwiftException("construct CephSwiftDataWriterImpl failed:container="
                    + containerName + ",object=" + objectName, e);
        }
    }

    @Override
    public void write(byte[] content) throws CephSwiftException {
        write(content, 0, content.length);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] content, int offset, int len) throws CephSwiftException {
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
        catch (CephSwiftException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("write data failed:container=" + containerName + ",object=" + objectName);
            throw new CephSwiftException(
                    "write data failed:container=" + containerName + ",object=" + objectName, e);
        }
    }

    private void sendAndClearBuffer() throws CephSwiftException {
        try {
            StoredObject obj = dataService.getObjectSegment(container, objectName, partNum);
            dataService.uploadObject(obj, new UploadInstructions(buffer));
        }
        catch (CephSwiftException e) {
            if (e.getSwiftErrorCode().equals(CephSwiftException.ERR_ENTITY_DOES_NOT_EXIST)) {
                // when occur this exception,current account may be become
                // unusable,maybe it's because joss's bug,so create a new
                // account
                this.account = dataService.createAccount();
                this.container = account.getContainer(containerName);

                boolean hasCreatedContainer = dataService.createContainer(container);
                if (hasCreatedContainer) {
                    this.createdContainerName = containerName;
                }
                StoredObject obj = dataService.getObjectSegment(container, objectName, partNum);
                dataService.uploadObject(obj, new UploadInstructions(buffer));
            }
            else {
                logger.error("upload segment object failed:container=" + containerName + ",object="
                        + objectName + ",part=" + partNum);
                throw e;
            }
        }
        partNum++;
        bufferOff = 0;
    }

    @Override
    @SlowLog(operation = "cancelWriter")
    public void cancel() {
        for (int i = 1; i < partNum; i++) {
            try {
                StoredObject segObject = dataService.getObjectSegment(container, objectName, i);
                dataService.deleteObject(segObject);
            }
            catch (Exception e) {
                logger.warn("clear obj failed:container={},objName={},partNum={}",
                        container.getName(), objectName, i, e);
            }
        }
        releaseResource();
    }

    private void releaseResource() {
        if (buffer != null) {
            poolWrapper.releaseBytes(buffer);
            buffer = null;
        }
        container = null;
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws CephSwiftException {
        try {
            StoredObject obj = dataService.getObject(container, objectName);
            // if file is empty or only one part,do not create manifest
            // object,just
            // create a normal object
            if (partNum < 2) {
                try {
                    dataService.uploadObject(obj,
                            new UploadInstructions(new ByteArrayInputStream(buffer, 0, bufferOff)));
                }
                catch (CephSwiftException e) {
                    if (e.getSwiftErrorCode()
                            .equals(CephSwiftException.ERR_ENTITY_DOES_NOT_EXIST)) {
                        // when occur this exception,current account may be
                        // become unusable,maybe it's because joss's bug,so
                        // create a new account
                        this.account = dataService.createAccount();
                        this.container = account.getContainer(containerName);

                        boolean hasCreatedContainer = dataService.createContainer(container);
                        if (hasCreatedContainer) {
                            this.createdContainerName = containerName;
                        }
                        obj = dataService.getObject(container, objectName);
                        dataService.uploadObject(obj, new UploadInstructions(
                                new ByteArrayInputStream(buffer, 0, bufferOff)));
                    }
                    else {
                        throw e;
                    }
                }
            }
            else {
                if (bufferOff > 0) {
                    StoredObject segObj = dataService.getObjectSegment(container, objectName,
                            partNum);
                    dataService.uploadObject(segObj,
                            new UploadInstructions(new ByteArrayInputStream(buffer, 0, bufferOff)));
                    partNum++;
                }

                ObjectMetadata segCountMeta = new ObjectMetadata(
                        CephSwiftCommonDefine.SEGMENT_COUNT, partNum - 1 + "");
                ObjectMetadata fileSizeMeta = new ObjectMetadata(CephSwiftCommonDefine.FILE_SIZE,
                        fileSize + "");
                dataService.uploadObject(obj, new UploadInstructions(new byte[0])
                        .setObjectManifest(new ObjectManifest(containerName + "/" + objectName))
                        .addHeader(segCountMeta).addHeader(fileSizeMeta));
            }
        }
        catch (CephSwiftException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("close CephSwiftDataWriter failed:container=" + containerName + ",object="
                    + objectName);
            throw new CephSwiftException("close CephSwiftDataWriter failed:container="
                    + containerName + ",object=" + objectName, e);
        }
        finally {
            releaseResource();
        }
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    @Override
    public String getCreatedTableName() {
        return createdContainerName;
    }

}
