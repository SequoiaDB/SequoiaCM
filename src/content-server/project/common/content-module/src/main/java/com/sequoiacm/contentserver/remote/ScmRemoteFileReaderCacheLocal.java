package com.sequoiacm.contentserver.remote;

import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;

/*
 * read file from remote site and cache file locally
 */
public class ScmRemoteFileReaderCacheLocal extends ScmFileReader {
    private static final Logger logger = LoggerFactory.getLogger(ScmRemoteFileReaderCacheLocal.class);
    private String fileId;
    private int majorVersion;
    private int minorVersion;

    private int localSiteId;
    private ScmWorkspaceInfo wsInfo;
    private String workspaceName;
    private ScmDataWriter fileWriter = null;
    private ScmDataInfo fileWriterDataInfo;

    private ScmRemoteFileReader remoteReader;
    private int remoteSiteId;

    public ScmRemoteFileReaderCacheLocal(String sessionId, String userDetail, int localSiteId, int remoteSiteId,
            ScmWorkspaceInfo wsInfo, String fileId, int majorVersion, int minorVersion,
            ScmDataWriter fileWriter, int flag, ScmDataInfo fileWriterDataInfo) throws ScmServerException {
        this.localSiteId = localSiteId;
        this.remoteSiteId = remoteSiteId;
        this.fileId = fileId;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.wsInfo = wsInfo;
        this.workspaceName = wsInfo.getName();

        this.fileWriter = fileWriter;
        this.fileWriterDataInfo = fileWriterDataInfo;
        this.remoteReader = new ScmRemoteFileReader(sessionId, userDetail, remoteSiteId, wsInfo, fileId, majorVersion, minorVersion, flag);
    }



    private void finishLocalLob() {
        try {
            FileCommonOperator.closeWriter(fileWriter);
            FileCommonOperator.addSiteInfoToList(wsInfo,
                    fileId, majorVersion, minorVersion, localSiteId, fileWriterDataInfo.getWsVersion());
        }
        catch (Exception e) {
            // we ignore this exception because our purpose is read file, not
            // write file
            logger.warn("finish file failed:fileId=" + fileId + ",site=" + localSiteId, e);
        }

    }



    @Override
    public void close() {
        try {
            if (isEof()) {
                // normal close
                finishLocalLob();
            }
            else {
                // interrupted
                cancel();
            }
        }
        finally {
            FileCommonOperator.recordDataTableName(wsInfo.getName(), fileWriter);
            fileWriter = null;
            remoteReader.close();
        }
    }

    private void cancel() {
        // cancel will interrupt Sequoiadb connection
        FileCommonOperator.cancelWriter(fileWriter);
        remoteReader.close();
    }


    @Override
    public int read(byte[] buff, int offset, int len) throws ScmServerException {
        if (isEof()) {
            return -1;
        }

        try {
            int actLen = remoteReader.read(buff, offset, len);
            if(actLen != -1) {
                fileWriter.write(buff, offset, actLen);
            }
            return actLen;
        }
        catch (ScmServerException e) {
            cancel();
            throw e;
        }
        catch (Exception e) {
            cancel();
            throw new ScmSystemException(
                    "read file from remote failed:remote=" + remoteSiteId + ",fileId=" + fileId, e);
        }
    }

    @Override
    public void seek(long size) throws ScmServerException {
        cancel();
        throw new ScmOperationUnsupportedException("can't seek in remote"
                + ",fileId=" + fileId);
    }

    @Override
    public boolean isEof() {
        return remoteReader.isEof();
    }



    @Override
    public long getSize() throws ScmServerException {
        return remoteReader.getSize();
    }


}
