package com.sequoiacm.client.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import java.util.zip.Checksum;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmChecksumFactory;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.BreakpointInputStream;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;

class ScmBreakpointFileImpl implements ScmBreakpointFile {
    private static final Logger logger = LoggerFactory.getLogger(ScmBreakpointFileImpl.class);
    private static final int DEFAULT_BREAKPOINT_SIZE = 1024 * 1024 * 16; // 16MB
    private static final int MIN_BREAKPOINT_SIZE = 1024 * 1024; // 1MB

    private final ScmWorkspace workspace;
    private String fileName;
    private String siteName;
    private ScmChecksumType checksumType;
    private long checksum;
    private String dataId;
    private boolean completed;
    private long uploadSize;
    private String createUser;
    private long createTime = 0;
    private String uploadUser;
    private long uploadTime;
    private boolean isNew;
    private int breakpointSize;

    ScmBreakpointFileImpl(ScmWorkspace workspace, BSONObject obj, int breakpointSize)
            throws ScmException {
        if (workspace == null) {
            throw new ScmInvalidArgumentException("workspace is null");
        }
        if (obj == null) {
            throw new ScmInvalidArgumentException("obj is null");
        }

        fromBSONObj(obj);
        this.workspace = workspace;
        this.isNew = false;
        this.breakpointSize = Math.max(breakpointSize, MIN_BREAKPOINT_SIZE);
    }

    ScmBreakpointFileImpl(ScmWorkspace workspace, BSONObject obj) throws ScmException {
        this(workspace, obj, DEFAULT_BREAKPOINT_SIZE);
    }

    ScmBreakpointFileImpl(ScmWorkspace workspace, String fileName, ScmChecksumType checksumType,
            int breakpointSize) throws ScmException {
        if (workspace == null) {
            throw new ScmInvalidArgumentException("workspace is null");
        }

        if (fileName == null || fileName.isEmpty()) {
            throw new ScmInvalidArgumentException("fileName is null or empty");
        }

        if (checksumType == null) {
            throw new ScmInvalidArgumentException("checksumType is null");
        }

        this.workspace = workspace;
        this.fileName = fileName;
        this.checksumType = checksumType;
        this.isNew = true;
        this.breakpointSize = Math.max(breakpointSize, MIN_BREAKPOINT_SIZE);
    }

    ScmBreakpointFileImpl(ScmWorkspace workspace, String fileName, ScmChecksumType checksumType)
            throws ScmException {
        this(workspace, fileName, checksumType, DEFAULT_BREAKPOINT_SIZE);
    }

    private void fromBSONObj(BSONObject obj) throws ScmException {
        fileName = BsonUtils.getStringChecked(obj, FieldName.BreakpointFile.FIELD_FILE_NAME);
        siteName = BsonUtils.getStringChecked(obj, FieldName.BreakpointFile.FIELD_SITE_NAME);
        checksumType = ScmChecksumType.valueOf(BsonUtils.getStringChecked(obj,
                FieldName.BreakpointFile.FIELD_CHECKSUM_TYPE));
        checksum = BsonUtils.getNumberChecked(obj, FieldName.BreakpointFile.FIELD_CHECKSUM)
                .longValue();
        dataId = BsonUtils.getString(obj, FieldName.BreakpointFile.FIELD_DATA_ID);
        completed = BsonUtils.getBooleanChecked(obj, FieldName.BreakpointFile.FIELD_COMPLETED);
        uploadSize = BsonUtils.getNumberChecked(obj, FieldName.BreakpointFile.FIELD_UPLOAD_SIZE)
                .longValue();
        createUser = BsonUtils.getString(obj, FieldName.BreakpointFile.FIELD_CREATE_USER);
        createTime = BsonUtils.getNumberOrElse(obj, FieldName.BreakpointFile.FIELD_CREATE_TIME, 0L)
                .longValue();
        uploadUser = BsonUtils.getString(obj, FieldName.BreakpointFile.FIELD_UPLOAD_USER);
        uploadTime = BsonUtils.getNumberOrElse(obj, FieldName.BreakpointFile.FIELD_UPLOAD_TIME, 0L)
                .longValue();
    }

    @Override
    public ScmWorkspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public ScmChecksumType getChecksumType() {
        return checksumType;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    @Override
    public String getDataId() {
        return dataId;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public long getUploadSize() {
        return uploadSize;
    }

    @Override
    public String getCreateUser() {
        return createUser;
    }

    @Override
    public Date getCreateTime() {
        return new Date(createTime);
    }

    @Override
    public void setCreateTime(Date createTime) throws ScmException {
        if (!isNew) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "can't set create time when file is exist");
        }

        this.createTime = createTime.getTime();
    }

    @Override
    public String getUploadUser() {
        return uploadUser;
    }

    @Override
    public Date getUploadTime() {
        return new Date(uploadTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ScmBreakpointFile) {
            ScmBreakpointFile file = (ScmBreakpointFile) obj;
            if (this.workspace.getName().equals(file.getWorkspace().getName())
                    && this.fileName.equals(file.getFileName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace.getName(), fileName);
    }

    @Override
    public void upload(InputStream dataStream) throws ScmException {
        if (dataStream == null) {
            throw new ScmInvalidArgumentException("fileStream is null");
        }

        if (isCompleted()) {
            throw new ScmInvalidArgumentException("The file is already completed");
        }

        if (uploadSize > 0) {
            long checksum = calcChecksum(checksumType, dataStream, uploadSize);
            if (checksum != this.checksum) {
                throw new ScmInvalidArgumentException("Checksum is different with server side");
            }
        }

        incrementalUpload(dataStream, true);
    }

    @Override
    public void upload(File file) throws ScmException {
        FileInputStream fileStream;
        try {
            fileStream = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new ScmInvalidArgumentException(e.getMessage(), e);
        }

        try {
            upload(fileStream);
        }
        finally {
            try {
                fileStream.close();
            }
            catch (IOException e) {
                logger.error("Failed to close file input stream", e);
            }
        }
    }

    @Override
    public void incrementalUpload(InputStream dataStream, boolean isLastContent)
            throws ScmException {
        if (dataStream == null) {
            throw new ScmInvalidArgumentException("fileStream is null");
        }

        if (isCompleted()) {
            throw new ScmInvalidArgumentException("The file is already completed");
        }

        if (isNew) {
            BSONObject obj = workspace
                    .getSession()
                    .getDispatcher()
                    .createBreakpointFile(workspace.getName(), fileName, createTime, checksumType,
                            null, false);
            fromBSONObj(obj);
            isNew = false;
        }

        BreakpointInputStream stream = new BreakpointInputStream(dataStream, breakpointSize);
        while (!stream.isEof()) {
            BSONObject obj = workspace.getSession().getDispatcher()
                    .uploadBreakpointFile(workspace.getName(), fileName, stream, uploadSize, false);
            fromBSONObj(obj);
            stream.resetBreakpoint();
        }

        if (isLastContent) {
            BSONObject obj = workspace.getSession().getDispatcher()
                    .uploadBreakpointFile(workspace.getName(), fileName, stream, uploadSize, true);
            fromBSONObj(obj);
        }
    }

    private long calcChecksum(final ScmChecksumType checksumType, InputStream fileStream,
            final long checksumSize) throws ScmException {
        if (checksumSize <= 0) {
            return 0L;
        }

        Checksum cs = ScmChecksumFactory.getChecksum(checksumType);

        final int bufferSize = 1024 * 256;
        byte[] buffer = new byte[bufferSize];

        long remainSize = checksumSize;
        while (remainSize > 0) {
            try {
                int readSize = (int) Math.min(remainSize, bufferSize);
                int size = fileStream.read(buffer, 0, readSize);
                if (size <= 0) {
                    throw new ScmSystemException("Failed to read data from file stream");
                }

                remainSize -= size;
                cs.update(buffer, 0, size);
            }
            catch (IOException e) {
                throw new ScmSystemException("Failed to read data from file stream", e);
            }
        }

        return cs.getValue();
    }
}
