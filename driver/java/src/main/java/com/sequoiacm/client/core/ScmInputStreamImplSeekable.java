package com.sequoiacm.client.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import com.sequoiacm.client.common.ClientDefine;
import com.sequoiacm.client.dispatcher.CloseableFileDataEntity;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;

class ScmInputStreamImplSeekable implements ScmInputStream, Closeable {
    private ScmFile scmFile;
    private ScmSession session;
    private boolean isClosed;
    private long currentOffset;

    public ScmInputStreamImplSeekable(ScmFile scmFile) throws ScmException {
        if (scmFile == null) {
            throw new ScmInvalidArgumentException("scmFile is null");
        }
        if (!scmFile.isExist()) {
            throw new ScmInvalidArgumentException("file is non-existing");
        }
        isClosed = false;
        currentOffset = 0;
        this.scmFile = scmFile;
        session = scmFile.getSession();
    }

    @Override
    public void seek(int seekType, long size) throws ScmException {
        if (isClosed == true) {
            throw new ScmException(ScmError.INPUT_STREAM_CLOSED, "Stream Closed");
        }

        if (seekType != CommonDefine.SeekType.SCM_FILE_SEEK_SET) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "supported seek type:SCM_FILE_SEEK_SET");
        }

        if (size > scmFile.getSize()) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "invalid seek size:fileSzie=" + scmFile.getSize() + ",seekSize=" + size);
        }

        this.currentOffset = size;
    }

    @Override
    public void read(OutputStream out) throws ScmException {
        if (isClosed == true) {
            throw new ScmException(ScmError.INPUT_STREAM_CLOSED, "Stream Closed");
        }

        if (out == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "outputstream is null");
        }

        if (currentOffset != 0 && currentOffset == scmFile.getSize()) {
            return;
        }

        if (currentOffset > scmFile.getSize()) {
            // should not come here
            throw new ScmSystemException("currentOffset greater than file size:fileId="
                    + scmFile.getFileId() + ",version=" + scmFile.getMajorVersion() + "."
                    + scmFile.getMinorVersion() + ",currentOffset=" + currentOffset + ",fileSize="
                    + scmFile.getSize());
        }

        CloseableFileDataEntity fileDataEntity = null;
        try {
            fileDataEntity = session.getDispatcher().downloadFile(scmFile.getWorkspaceName(),
                    scmFile.getFileId().get(), scmFile.getMajorVersion(), scmFile.getMinorVersion(),
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK, currentOffset,
                    CommonDefine.File.UNTIL_END_OF_FILE);
            long fileDataLength = fileDataEntity.getLength();
            long totalReadLen = 0;
            byte[] buf = new byte[ClientDefine.File.TRANSMISSION_LEN];

            while (true) {
                int readLen = fileDataEntity.readAsMuchAsPossible(buf);
                if (readLen <= -1) {
                    break;
                }
                out.write(buf, 0, readLen);
                totalReadLen += readLen;
            }
            if (fileDataLength != totalReadLen) {
                throw new ScmException(ScmError.DATA_CORRUPTED,
                        "data is incomplete:fileId=" + scmFile.getFileId().get() + ",version="
                                + scmFile.getMajorVersion() + "." + scmFile.getMinorVersion()
                                + ",expectLen=" + fileDataLength + ",actualLen=" + totalReadLen);
            }
            currentOffset += totalReadLen;
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO,
                    "failed to read file:fileId=" + scmFile.getFileId().get() + ",version="
                            + scmFile.getMajorVersion() + "." + scmFile.getMinorVersion(),
                            e);
        }
        finally {
            ScmHelper.closeStream(fileDataEntity);
        }

    }

    @Override
    public int read(byte[] b, int off, int len) throws ScmException {
        if (isClosed == true) {
            throw new ScmException(ScmError.INPUT_STREAM_CLOSED, "Stream Closed");
        }
        if (b == null) {
            throw new ScmInvalidArgumentException("byteArray is null");
        }
        if (len <= 0) {
            throw new ScmInvalidArgumentException("len must be greater than zero:" + len);
        }
        if (off + len > b.length || off < 0) {
            throw new ScmInvalidArgumentException(
                    "indexOutOfBound,arraySize:" + b.length + ",off:" + off + ",len:" + len);
        }

        if (currentOffset != 0 && currentOffset == scmFile.getSize()) {
            return -1;
        }

        if (currentOffset > scmFile.getSize()) {
            // should not come here
            throw new ScmSystemException("currentOffset greater than file size:fileId="
                    + scmFile.getFileId() + ",version=" + scmFile.getMajorVersion() + "."
                    + scmFile.getMinorVersion() + ",currentOffset=" + currentOffset + ",fileSize="
                    + scmFile.getSize());
        }

        int expectLen = len > ClientDefine.File.TRANSMISSION_LEN
                ? ClientDefine.File.TRANSMISSION_LEN
                        : len;
        CloseableFileDataEntity fileDataEntity = null;
        try {
            fileDataEntity = session.getDispatcher().downloadFile(scmFile.getWorkspaceName(),
                    scmFile.getFileId().get(), scmFile.getMajorVersion(), scmFile.getMinorVersion(),
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK, currentOffset, expectLen);
            long fileDataLength = fileDataEntity.getLength();
            if (fileDataLength <= -1) {
                return -1;
            }
            if (fileDataLength > len) {
                // impossible
                throw new ScmSystemException("return data size greater than expected size");
            }
            int totalReadLen = 0;
            while (true) {
                int readLen = fileDataEntity.read(b, off, len - totalReadLen);
                if (readLen <= -1) {
                    break;
                }
                off += readLen;
                totalReadLen += readLen;
            }
            if (totalReadLen != fileDataLength) {
                throw new ScmException(ScmError.DATA_CORRUPTED,
                        "data is incomplete:fileId=" + scmFile.getFileId().get() + ",version="
                                + scmFile.getMajorVersion() + "." + scmFile.getMinorVersion()
                                + ",expectLen=" + fileDataLength + ",actualLen=" + totalReadLen);
            }
            currentOffset += totalReadLen;
            return totalReadLen;
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO,
                    "failed to read file:fileId=" + scmFile.getFileId().get() + ",version="
                            + scmFile.getMajorVersion() + "." + scmFile.getMinorVersion(),
                            e);
        }
        finally {
            ScmHelper.closeStream(fileDataEntity);
        }
    }

    @Override
    public void close() {
        isClosed = true;
        // no resource to release
    }
}
