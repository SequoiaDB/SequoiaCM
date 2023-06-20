package com.sequoiacm.contentserver.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import com.sequoiacm.contentserver.model.DataTableDeleteOption;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.DatalocationFactory;
import com.sequoiacm.datasource.metadata.ScmLocation;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
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
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataInfoFetcher;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;

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
            long createTime, int readflag, OutputStream os, int wsVersion, String tableName)
            throws ScmServerException {
        ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(dataType, dataId, new Date(createTime),
                wsVersion, tableName);
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
    public void createDataInLocal(String wsName, ScmDataInfo dataInfo, ScmDataWriterContext context)
            throws ScmServerException {
        createDataInLocal(wsName, dataInfo, null, context);
    }

    @Override
    public void createDataInLocal(String wsName, ScmDataInfo dataInfo, InputStream is,
            ScmDataWriterContext context)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        // ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new
        // Date(createTime), wsVersion);

        ScmDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createWriter(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), contentModule.getDataService(),
                    dataInfo, context);
        }
        catch (ScmDatasourceException e) {
            BSONObject extraInfo = new BasicBSONObject();
            extraInfo.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME, context.getTableName());
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e, extraInfo);
        }

        // get buffer from ScmMemoryPool
        byte[] buf = null;
        ScmPoolWrapper poolWrapper = null;
        try {
            poolWrapper = ScmPoolWrapper.getInstance();
            buf = poolWrapper.getBytes(Const.TRANSMISSION_LEN);
        }
        catch (Exception e) {
            throw new ScmSystemException("Failed to get buffer from scm memory pool, bufferSize: "
                    + Const.TRANSMISSION_LEN + ", cause by: " + e.getMessage(), e);
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
            BSONObject extraInfo = new BasicBSONObject();
            extraInfo.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME, context.getTableName());
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to write data", e, extraInfo);
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

            // give back buffer to ScmMemoryPool
            if (buf != null) {
                poolWrapper.releaseBytes(buf);
            }
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

        ScmDataInfo dataInfo = ScmDataInfo.forCreateNewData(ENDataType.Normal.getValue(), dataId,
                createTime,
                wsInfo.getVersion());
        ScmDataInfoDetail scmDataInfoDetail = new ScmDataInfoDetail(dataInfo);

        InputStreamWithCalcMd5 inputStreamWithCalcMd5 = new InputStreamWithCalcMd5(data, false);
        ScmDataWriterContext context = new ScmDataWriterContext();
        try {
            createDataInLocal(wsName, dataInfo, inputStreamWithCalcMd5, context);
            scmDataInfoDetail.setMd5(inputStreamWithCalcMd5.calcMd5());
            scmDataInfoDetail.setSize(inputStreamWithCalcMd5.getSize());
        }
        finally {
            ScmSystemUtils.closeResource(inputStreamWithCalcMd5);
        }
        scmDataInfoDetail.setSiteId(ScmContentModule.getInstance().getLocalSite());
        dataInfo.setTableName(context.getTableName());
        return scmDataInfoDetail;
    }

    @Override
    public void deleteDataTables(List<String> tableNames, String wsName, ScmLocation location)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmDataTableDeletor deletor;
        try {
            deletor = ScmDataOpFactoryAssit.getFactory().createDataTableDeletor(tableNames,
                    contentModule.getDataService(), location, wsName);
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to delete data tables:" + tableNames, e);
        }
    }

    @Override
    public ScmDataReader getScmDataReader(String wsName, String dataId, int dataType,
            long createTime, int wsVersion, String tableName) throws ScmServerException, ScmDatasourceException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(dataType, dataId, new Date(createTime),
                wsVersion, tableName);

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
            int dataType, long createTime, int wsVersion, String tableName) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);

        ScmSeekableDataWriter writer = null;
        ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(ENDataType.Normal.getValue(), dataId,
                new Date(createTime), wsVersion, tableName);
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createSeekableDataWriter(
                    wsInfo.getDataLocation(wsVersion), contentModule.getDataService(), wsName, null,
                    dataInfo, false, 0, null);
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
        ScmFileDataDeleter fileDataDeleter = new ScmFileDataDeleter(siteList, wsInfo, dataId, type,
                new Date(createTime));
        fileDataDeleter.deleteData();
    }
}
