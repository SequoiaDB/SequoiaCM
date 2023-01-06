package com.sequoiacm.contentserver.remote;

import java.io.IOException;
import java.io.InputStream;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;

import feign.Response;

public class ScmInnerRemoteDataReader {
    private static final Logger logger = LoggerFactory.getLogger(ScmInnerRemoteDataReader.class);

    private ScmDataInfo dataInfo;
    private int flag;
    private boolean isEof = false;
    private String remoteSiteName;
    private InputStream is;
    private Response resp;
    private ContentServerClient client;
    private ScmWorkspaceInfo wsInfo;

    private long expectDataLen;
    private long actReadLen;

    @SlowLog(operation = "openReader", extras = {
            @SlowLogExtra(name = "readFileId", data = "dataInfo.getId()"),
            @SlowLogExtra(name = "readRemoteSiteName", data = "remoteSiteName") })
    public ScmInnerRemoteDataReader(int remoteSiteId, ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo,
            int flag, int targetSiteId) throws ScmServerException {
        this.dataInfo = dataInfo;
        this.flag = flag & ~CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA;
        this.wsInfo = wsInfo;
        ScmContentModule contentModule = ScmContentModule.getInstance();
        String targetSiteName = contentModule.getSiteInfo(targetSiteId).getName();
        remoteSiteName = contentModule.getSiteInfo(remoteSiteId).getName();

        try {
            client = ContentServerClientFactory.getFeignClientByServiceName(remoteSiteName);
            resp = client.readData(wsInfo.getName(), targetSiteName, dataInfo.getId(),
                    dataInfo.getType(), dataInfo.getCreateTime().getTime(), flag,
                    dataInfo.getWsVersion(), dataInfo.getTableName());
            RemoteCommonUtil.checkResponse("readData", resp);
            expectDataLen = Long.valueOf(
                    RemoteCommonUtil.firstOrNull(resp.headers(), CommonDefine.RestArg.DATA_LENGTH));
            is = resp.body().asInputStream();
        }
        catch (ScmServerException e) {
            logger.error("read data from remote failed:ws=" + wsInfo.getName() + ",remote="
                    + remoteSiteName + ",datainfo=" + dataInfo);
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("read data from remote failed:ws=" + wsInfo.getName()
                    + ",remote=" + remoteSiteName + ",dataInfo" + dataInfo, e);
        }
    }

    public ScmInnerRemoteDataReader(int remoteSiteId, ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo,
            int flag) throws ScmServerException {
        this(remoteSiteId, wsInfo, dataInfo, flag, remoteSiteId);
    }

    @SlowLog(operation = "readData")
    public int read(byte[] buff, int offset, int len) throws ScmServerException {
        if (isEof) {
            return -1;
        }
        try {
            int actLen = CommonHelper.readAsMuchAsPossible(is, buff, offset, len);
            if (actLen == -1) {
                isEof = true;
                if (actReadLen != expectDataLen) {
                    throw new ScmServerException(ScmError.DATA_CORRUPTED,
                            "read data from remote is corrupted:ws=" + wsInfo.getName() + ",remote="
                                    + remoteSiteName + ",dataInfo=" + dataInfo + ", expectDataLen="
                                    + expectDataLen + ", actDataLen=" + actReadLen);
                }
            }
            actReadLen += actLen;
            return actLen;
        }
        catch (IOException e) {
            close();
            throw new ScmServerException(ScmError.NETWORK_IO, "read data from remote failed:ws="
                    + wsInfo.getName() + ",remote=" + remoteSiteName + "," + dataInfo, e);
        }
        catch (Exception e) {
            close();
            throw new ScmSystemException("read data from remote failed:ws=" + wsInfo.getName()
                    + ",remote=" + remoteSiteName + "," + dataInfo, e);
        }
    }

    public void seek(long size) throws ScmServerException {
        if ((flag & CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK) > 0) {
            //TODO:unsuppored seek
            throw new ScmOperationUnsupportedException(
                    "this remote can't seek:remote=" + remoteSiteName + "," + dataInfo);
        }
        else {
            logger.error("this remote can't seek:remote=" + remoteSiteName + "," + dataInfo);
            throw new ScmSystemException(
                    "this remote can't seek:remote=" + remoteSiteName + "," + dataInfo);
        }
    }

    public long getExpectDataLen() {
        return expectDataLen;
    }
    
    @SlowLog(operation = "closeReader")
    public void close() {
        try {
            if (is != null) {
                is.close();
            }
        }
        catch (Exception e) {
            logger.warn("close resource failed:ws={},remote={},dataInfo={}", wsInfo.getName(),
                    remoteSiteName, dataInfo, e);
        }

        try {
            if (resp != null) {
                resp.close();
            }
        }
        catch (Exception e) {
            logger.warn("close resource failed:ws={},remote={},dataInfo={}", wsInfo.getName(),
                    remoteSiteName, dataInfo, e);
        }
    }
}
