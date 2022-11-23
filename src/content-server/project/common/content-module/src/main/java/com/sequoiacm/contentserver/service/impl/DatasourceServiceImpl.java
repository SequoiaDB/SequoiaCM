package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.InputStreamWithCalcMd5;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.dao.ScmFileDataDeleter;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataDeletor;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.*;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

@Service
public class DatasourceServiceImpl implements IDatasourceService {
    private static final Logger logger = LoggerFactory.getLogger(DatasourceServiceImpl.class);

    @Override
    public void deleteDataLocal(String wsName, ScmDataInfo dataInfo) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(wsName);

        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(), dataInfo);
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                    "Failed to delete data", e);
        }
    }

    @Override
    public void deleteData(String wsName, ScmDataInfo dataInfo, int siteId)
            throws ScmServerException {
        if (siteId == ScmContentModule.getInstance().getLocalSite()) {
            deleteDataLocal(wsName, dataInfo);
            return;
        }

        if (siteId != ScmContentModule.getInstance().getMainSite()) {
            throw new ScmSystemException(
                    "only support delete local site or root site data: site=" + siteId);
        }

        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(wsName);
        ScmInnerRemoteDataDeletor deleter = new ScmInnerRemoteDataDeletor(siteId, wsInfo, dataInfo);
        deleter.delete();
    }

    @Override
    public DatasourceReaderDao readData(String workspaceName, String dataId, int dataType,
            long createTime, int readflag, OutputStream os, int wsVersion)
            throws ScmServerException {
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime), wsVersion);
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        // readflag no need process in current version

        return new DatasourceReaderDao(wsInfo, dataInfo);
    }

    @Override
    public BSONObject getDataInfo(String workspaceName, ScmDataInfo dataInfo)
            throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(workspaceName);
        ScmDataInfoFetcher fetcher;
        try {
            fetcher = ScmDataOpFactoryAssit.getFactory().createDataInfoFetcher(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(), dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to create data info fetcher", e);
        }
        long size = fetcher.getDataSize();
        BSONObject retInfo = new BasicBSONObject();
        retInfo.put(CommonDefine.RestArg.DATASOURCE_DATA_SIZE, size);
        return retInfo;

    }

    @Override
    public void createDataInLocal(String wsName, ScmDataInfo dataInfo) throws ScmServerException {
        createDataInLocal(wsName, dataInfo, null);
    }

    @Override
    public void createDataInLocal(String wsName, ScmDataInfo dataInfo, InputStream is)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        // ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new
        // Date(createTime), wsVersion);

        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        ScmDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createWriter(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), contentModule.getDataService(),
                    dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }
        try {
            if (is != null) {
                while (true) {
                    int actLen = CommonHelper.readAsMuchAsPossible(is, buf);
                    if (actLen == -1) {
                        break;
                    }
                    writer.write(buf, 0, actLen);
                    if (actLen < buf.length) {
                        break;
                    }
                }
            }
            writer.close();
        }
        catch (ScmDatasourceException e) {
            writer.cancel();
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to write data", e);
        }
        catch (IOException e) {
            writer.cancel();
            throw new ScmServerException(ScmError.NETWORK_IO,
                    "create data failed:wsName=" + wsName + ",dataInfo=" + dataInfo, e);
        }
        catch (Exception e) {
            writer.cancel();
            throw new ScmSystemException(
                    "create data failed:wsName=" + wsName + ",dataInfo=" + dataInfo, e);
        }
        finally {
            FileCommonOperator.recordDataTableName(wsName, writer);
        }
    }

    @Override
    @SlowLog(operation = "createData")
    public ScmDataInfoDetail createData(String wsName, InputStream data, long createTimeMill)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        Date createTime = new Date(createTimeMill);
        String dataId = ScmIdGenerator.FileId.get(createTime);

        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), dataId, createTime,
                wsInfo.getVersion());
        ScmDataInfoDetail scmDataInfoDetail = new ScmDataInfoDetail(dataInfo);

        InputStreamWithCalcMd5 inputStreamWithCalcMd5 = new InputStreamWithCalcMd5(data, false);
        try {
            createDataInLocal(wsName, dataInfo, inputStreamWithCalcMd5);
            scmDataInfoDetail.setMd5(inputStreamWithCalcMd5.calcMd5());
            scmDataInfoDetail.setSize(inputStreamWithCalcMd5.getSize());
        }
        finally {
            ScmSystemUtils.closeResource(inputStreamWithCalcMd5);
        }
        scmDataInfoDetail.setSiteId(ScmContentModule.getInstance().getLocalSite());
        return scmDataInfoDetail;
    }

    @Override
    public void deleteDataTables(List<String> tableNames) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmDataTableDeletor deletor;
        try {
            deletor = ScmDataOpFactoryAssit.getFactory().createDataTableDeletor(tableNames,
                    contentModule.getDataService());
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to delete data tables:" + tableNames, e);
        }
    }

    @Override
    public ScmDataReader getScmDataReader(String wsName, String dataId, int dataType,
            long createTime, int wsVersion) throws ScmServerException, ScmDatasourceException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime), wsVersion);

        ScmDataReader dataReader = null;
        try {
            dataReader = ScmDataOpFactoryAssit.getFactory().createReader(
                    contentModule.getLocalSite(), wsName, wsInfo.getDataLocation(wsVersion),
                    contentModule.getDataService(), dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to create data reader", e);
        }
        return dataReader;
    }

    @Override
    public ScmSeekableDataWriter getScmSeekableDataWriter(String wsName, String dataId,
            int dataType, long createTime, int wsVersion) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);

        ScmSeekableDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createSeekableDataWriter(
                    wsInfo.getDataLocation(wsVersion), contentModule.getDataService(), wsName, null, dataId,
                    new Date(createTime), false, 0, null);
            return writer;
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }
    }

    @Override
    public void deleteDataInSiteList(String wsName, String dataId, int type, long createTime,
            List<ScmFileLocation> siteList) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(type, dataId, new Date(createTime), 1);
        ScmFileDataDeleter fileDataDeleter = new ScmFileDataDeleter(siteList, wsInfo, dataInfo);
        fileDataDeleter.deleteData();
    }
}
