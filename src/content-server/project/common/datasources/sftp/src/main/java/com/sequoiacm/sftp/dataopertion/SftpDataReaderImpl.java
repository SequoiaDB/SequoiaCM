package com.sequoiacm.sftp.dataopertion;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.sequoiacm.datasource.common.ScmInputStreamDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.sftp.SftpDataException;
import com.sequoiacm.sftp.dataservice.SftpDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class SftpDataReaderImpl implements ScmDataReader {

    private Logger logger = LoggerFactory.getLogger(SftpDataReaderImpl.class);

    private ChannelSftp sftp;
    private ScmInputStreamDataReader inputStreamDataReader;

    private long size;
    private SftpDataService dataService;
    private String fileDir;
    private String fileName;

    @SlowLog(operation = "openReader", extras = {
            @SlowLogExtra(name = "readSftpFileDir", data = "fileDir"),
            @SlowLogExtra(name = "readSftpFileName", data = "fileName") })
    SftpDataReaderImpl(String fileDir, String fileName, ScmService service)
            throws SftpDataException {
        try {
            this.fileDir = fileDir;
            this.fileName = fileName;
            this.dataService = (SftpDataService) service;
            this.sftp = dataService.getSftp();
            this.size = sftp.lstat(fileDir + fileName).getSize();
            this.inputStreamDataReader = new ScmInputStreamDataReader(sftp.get(fileDir + fileName));
        }
        catch (SftpException e) {
            releaseResource();
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                throw new SftpDataException(SftpDataException.FILE_NOT_EXIST,
                        "file not exist, filePath=" + (fileDir + fileName), e);
            }
            throw new SftpDataException("failed to open file:" + (fileDir + fileName), e);
        }
    }

    private void releaseResource() {
        if (inputStreamDataReader != null) {
            try {
                inputStreamDataReader.close();
            }
            catch (Exception e) {
                logger.warn("failed to close stream, filePath=" + (fileDir + fileName), e);
            }
        }
        dataService.closeSftp(sftp);
    }

    @Override
    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws SftpDataException {

        try {
            return inputStreamDataReader.read(buff, offset, len);
        }
        catch (IOException e) {
            throw new SftpDataException("failed to read file:filePath=" + (fileDir + fileName)
                    + ",offset=" + offset + ",len=" + len, e);
        }
    }

    @Override
    @SlowLog(operation = "seekData")
    public void seek(long size) throws SftpDataException {
        try {
            inputStreamDataReader.seek(size);
        }
        catch (Exception e) {
            logger.error("seek data failed:filePath=" + (fileDir + fileName));
            throw new SftpDataException("seek data failed:filePath=" + (fileDir + fileName), e);
        }
    }

    @Override
    @SlowLog(operation = "closeReader")
    public void close() {
        releaseResource();
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
