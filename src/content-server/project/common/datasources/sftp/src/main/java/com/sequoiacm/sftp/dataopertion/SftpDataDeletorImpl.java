package com.sequoiacm.sftp.dataopertion;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.sftp.SftpDataException;
import com.sequoiacm.sftp.dataservice.SftpDataService;

public class SftpDataDeletorImpl implements ScmDataDeletor {
    private static final Logger logger = LoggerFactory.getLogger(SftpDataDeletorImpl.class);

    private String fileDir;
    private String fileName;
    private SftpDataService service;

    SftpDataDeletorImpl(String fileDir, String fileName, ScmService service)
            throws SftpDataException {
        this.fileDir = fileDir;
        this.fileName = fileName;
        this.service = (SftpDataService) service;
    }

    @Override
    @SlowLog(operation = "deleteData", extras = @SlowLogExtra(name = "deleteFileName", data = "fileName"))
    public void delete() throws SftpDataException {
        ChannelSftp sftp = service.getSftp();
        try {
            sftp.rm(fileDir + fileName);
        }
        catch (SftpException e) {
            if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                throw new SftpDataException(
                        "failed to delete file," + " filePath=" + (fileDir + fileName), e);
            }
        }
        finally {
            service.closeSftp(sftp);
        }
    }

}
