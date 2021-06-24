package com.sequoiacm.client.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ClientDefine;
import com.sequoiacm.client.dispatcher.CloseableFileDataEntity;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.exception.ScmError;

class ScmInputStreamImplUnseekable implements ScmInputStream, Closeable {
    private ScmFile scmFile;
    private ScmSession session;
    private CloseableFileDataEntity fileDataEntity;
    private boolean isClosed;
    private int totalLen;
    private int readFlag;

    private static final Logger logger = LoggerFactory.getLogger(ScmInputStreamImplUnseekable.class);

    public ScmInputStreamImplUnseekable(ScmFile scmFile, int readFlag) throws ScmException {
        if (scmFile == null) {
            throw new ScmInvalidArgumentException("scmFile is null");
        }
        if (!scmFile.isExist()) {
            throw new ScmInvalidArgumentException("file is non-existing");
        }
        this.readFlag = readFlag;
        isClosed = false;
        this.scmFile = scmFile;
        session = scmFile.getSession();

        fileDataEntity = session.getDispatcher().downloadFile(scmFile.getWorkspaceName(),
                scmFile.getFileId().get(), scmFile.getMajorVersion(), scmFile.getMinorVersion(), readFlag);
    }

    @Override
    public void read(OutputStream out) throws ScmException {
        checkStreamStat();
        if (out == null) {
            throw new ScmInvalidArgumentException("ouputStream is null");
        }
        try {
            int len = -1;
            byte[] buf = new byte[ClientDefine.File.MAX_READ_BUFFER_LEN];
            while ((len = fileDataEntity.readAsMuchAsPossible(buf)) != -1) {
                totalLen += len;
                out.write(buf, 0, len);
            }
            if(totalLen != scmFile.getSize()) {
                throw new ScmException(ScmError.DATA_CORRUPTED,
                        "data is incomplete:fileId=" + scmFile.getFileId().get() + ",expectLen=" + scmFile.getSize() + ",actualLen=" + totalLen);
            }
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO,
                    "outputStream has io error when wrote data", e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws ScmException {
        checkStreamStat();
        if (b == null) {
            throw new ScmInvalidArgumentException("byteArray is null");
        }
        if (len <= 0) {
            throw new ScmInvalidArgumentException(
                    "len must be greater than zero:" + len);
        }
        if (off + len > b.length || off < 0) {
            throw new ScmInvalidArgumentException(
                    "indexOutOfBound,arraySize:" + b.length + ",off:" + off + ",len:" + len);
        }
        int expectLen = len > ClientDefine.File.TRANSMISSION_LEN
                ? ClientDefine.File.TRANSMISSION_LEN
                        : len;
        try {
            int readLen = fileDataEntity.readAsMuchAsPossible(b, off, expectLen);
            if(readLen == -1) {
                if(totalLen != scmFile.getSize()) {
                    throw new ScmException(ScmError.DATA_CORRUPTED,
                            "data is incomplete:fileId=" + scmFile.getFileId().get() + "expectLen=" + scmFile.getSize() + ",actualLen=" + totalLen);
                }
            }

            totalLen += readLen;
            return readLen;
        }
        catch (IOException e) {
            throw new ScmException(ScmError.FILE_IO,
                    "outputStream has io error when wrote data", e);
        }
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        ScmHelper.closeStream(fileDataEntity);
        isClosed = true;
    }

    private void checkStreamStat() throws ScmException {
        if (isClosed) {
            throw new ScmException(ScmError.INPUT_STREAM_CLOSED, "Stream Closed");
        }
    }

    protected ScmFile getScmFile() {
        return scmFile;
    }

    protected ScmSession getSession() {
        return session;
    }

    @Override
    public void seek(int seekType, long size) throws ScmException {
        throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "unsupported opration");
    }

}
