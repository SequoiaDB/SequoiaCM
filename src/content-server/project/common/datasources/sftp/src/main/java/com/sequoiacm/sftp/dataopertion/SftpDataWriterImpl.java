package com.sequoiacm.sftp.dataopertion;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.sftp.SftpDataException;
import com.sequoiacm.sftp.dataservice.SftpDataService;

import java.io.IOException;
import java.io.OutputStream;

public class SftpDataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(SftpDataWriterImpl.class);

    private SftpDataService dataService;
    private ChannelSftp sftp;
    private long offset = 0;
    private String fileDir;
    private String fileName;
    private OutputStream outputStream;

    @SlowLog(operation = "openWriter", extras = {
            @SlowLogExtra(name = "writeSftpFileDir", data = "fileDir"),
            @SlowLogExtra(name = "writeSftpFileName", data = "fileName") })
    SftpDataWriterImpl(String fileDir, String fileName, ScmService service)
            throws SftpDataException {
        try {
            this.fileName = fileName;
            this.fileDir = fileDir;
            this.dataService = (SftpDataService) service;
            this.sftp = dataService.getSftp();
            if (isFileExist(fileDir + fileName)) {
                throw new SftpDataException(SftpDataException.FILE_EXIST,
                        "file already exist, filePath=" + (fileDir + fileName));
            }
            this.outputStream = createFile();
        }
        catch (SftpDataException e) {
            releaseResource();
            throw e;
        }
        catch (Exception e) {
            releaseResource();
            throw new SftpDataException("failed to create file, filePath=" + (fileDir + fileName),
                    e);
        }
    }

    private OutputStream createFile() throws SftpDataException {
        try {
            return sftp.put(fileDir + fileName);
        }
        catch (SftpException e) {
            // 目录不存在
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                createDir(fileDir);
                return createFile();
            }
            throw new SftpDataException("failed to create file, filePath=" + (fileDir + fileName),
                    e);
        }

    }

    private void createDir(String fileDir) throws SftpDataException {
        synchronized (SftpDataWriterImpl.class) {
            if (isDirExist(fileDir)) {
                return;
            }
            String[] pathArray = fileDir.split("/");
            StringBuilder sb = new StringBuilder("/");
            for (String pathItem : pathArray) {
                if (pathItem.isEmpty()) {
                    continue;
                }
                sb.append(pathItem).append("/");
                if (!isDirExist(sb.toString())) {
                    try {
                        sftp.mkdir(sb.toString());
                    }
                    catch (SftpException e) {
                        if (!isDirExist(sb.toString())) {
                            throw new SftpDataException("failed to create dir:" + fileDir, e);
                        }
                    }
                }
            }
        }

    }

    private boolean isDirExist(String fileDir) throws SftpDataException {
        try {
            SftpATTRS lstat = sftp.lstat(fileDir);
            return lstat.isDir();
        }
        catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            throw new SftpDataException("failed to check dir:" + fileDir, e);
        }
    }

    private boolean isFileExist(String filePath) throws SftpDataException {
        try {
            this.sftp.lstat(filePath);
            return true;
        }
        catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            throw new SftpDataException("failed to check file status, filePath=" + filePath, e);
        }
    }

    @Override
    public void write(byte[] content) throws SftpDataException {
        write(content, 0, content.length);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] content, int offset, int len) throws SftpDataException {
        try {
            outputStream.write(content, offset, len);
            this.offset += len;
        }
        catch (Exception e) {
            throw new SftpDataException("failed to write file, filePath=" + (fileDir + fileName),
                    e);
        }

    }

    @Override
    @SlowLog(operation = "cancelWriter")
    public void cancel() {
        try {
            if (sftp != null) {
                if (sftp.isClosed()) {
                    sftp = dataService.getSftp();
                }
                sftp.rm(fileDir + fileName);
            }
        }
        catch (SftpException e) {
            if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                logger.warn("failed to cancel write, filePath=" + (fileDir + fileName), e);
            }
        }
        catch (Exception e) {
            logger.warn("failed to cancel write, filePath=" + (fileDir + fileName), e);
        }
        try {
            releaseResource();
        }
        catch (Exception e) {
            logger.warn("failed to releaseResource, filePath=" + (fileDir + fileName), e);
        }

    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws SftpDataException {
        releaseResource();
    }

    private void releaseResource() throws SftpDataException {
        try {
            if (null != outputStream) {
                outputStream.close();
            }
        }
        catch (IOException e) {
            throw new SftpDataException("failed to close stream, filePath=" + (fileDir + fileName),
                    e);
        }
        finally {
            dataService.closeSftp(sftp);
        }
    }

    @Override
    public long getSize() {
        return offset;
    }

    @Override
    public String getCreatedTableName() {
        return null;
    }

}
