package com.sequoiacm.contentserver.remote;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmError;

import feign.Response;

/*
 * read file from remote site. do not cache file locally
 */
public class ScmRemoteFileReader extends ScmFileReader {
    private static final Logger logger = LoggerFactory.getLogger(ScmRemoteFileReader.class);
    private String fileId;
    private boolean isEof = false;
    private String workspaceName;
    private int flag;

    private InputStream is;
    private ScmSite remoteSite;
    private Response resp;
    private long expectDataLen;
    private long actReadLen;

    public ScmRemoteFileReader(String sessionId, String userDetail, int remoteSiteId,
            ScmWorkspaceInfo wsInfo, String fileId, int majorVersion, int minorVersion, int flag)
            throws ScmServerException {
        this.fileId = fileId;
        this.workspaceName = wsInfo.getName();
        this.flag = flag & ~CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA;
        ScmContentServer contentServer = ScmContentServer.getInstance();
        remoteSite = contentServer.getSiteInfo(remoteSiteId);

        try {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(remoteSite.getName());
            if (sessionId == null && userDetail == null) {
                resp = client.downloadFileInternal(wsInfo.getName(), fileId, majorVersion,
                        minorVersion, flag);
            }
            else {
                resp = client.downloadFile(sessionId, userDetail, wsInfo.getName(), fileId,
                        majorVersion, minorVersion, flag);
            }
            RemoteCommonUtil.checkResponse("downLoadFile", resp);
            expectDataLen = Long.valueOf(
                    RemoteCommonUtil.firstOrNull(resp.headers(), CommonDefine.RestArg.DATA_LENGTH));
            is = resp.body().asInputStream();
        }
        catch (ScmServerException e) {
            logger.error(
                    "read from remote failed:remote=" + remoteSite.getName() + ",fileId=" + fileId);
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("read file from remote failed:remote="
                    + remoteSite.getName() + ",workspace=" + workspaceName + ",fileId=" + fileId,
                    e);
        }
    }

    @Override
    public void close() {
        try {
            if (is != null) {
                is.close();
            }
        }
        catch (Exception e) {
            logger.warn("close resource failed:wsName={},fileId={}", workspaceName, fileId, e);
        }

        try {
            if (resp != null) {
                resp.close();
            }
        }
        catch (Exception e) {
            logger.warn("close resource failed:wsName={},fileId={}", workspaceName, fileId, e);
        }
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws ScmServerException {
        if (isEof) {
            return -1;
        }
        try {
            int actLen = is.read(buff, offset, len);
            if (actLen == -1) {
                isEof = true;
                if (actReadLen != expectDataLen) {
                    throw new ScmServerException(ScmError.DATA_CORRUPTED,
                            "read data from remote is corrupted:ws=" + workspaceName + ",remote="
                                    + remoteSite.getName() + ",fileId=" + fileId
                                    + ", expectDataLen=" + expectDataLen + ", actDataLen="
                                    + actReadLen);
                }
            }
            actReadLen += actLen;
            return actLen;
        }
        catch (IOException e) {
            close();
            throw new ScmServerException(ScmError.NETWORK_IO, "read file from remote failed:remote="
                    + remoteSite.getName() + ",fileId=" + fileId, e);
        }
        catch (Exception e) {
            close();
            throw new ScmSystemException("read file from remote failed:remote="
                    + remoteSite.getName() + ",fileId=" + fileId, e);
        }
    }

    @Override
    public void seek(long size) throws ScmServerException {
        if ((flag & CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK) > 0) {
            // TODO:unsupported seek now!
            throw new ScmOperationUnsupportedException("this remote can't seek:remote="
                    + remoteSite.getName() + ",wsName=" + workspaceName + ",fileId=" + fileId);
        }
        else {
            logger.error("this remote can't seek:remote=" + remoteSite.getName() + ",wsName="
                    + workspaceName + ",fileId=" + fileId);
            throw new ScmOperationUnsupportedException("this remote can't seek:remote="
                    + remoteSite.getName() + ",wsName=" + workspaceName + ",fileId=" + fileId);
        }
    }

    @Override
    public boolean isEof() {
        return isEof;
    }

    @Override
    public long getSize() {
        return expectDataLen;
    }
}
