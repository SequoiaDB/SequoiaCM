package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
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
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataWriter;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.*;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
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
    public void deleteDataLocal(String wsName, String dataId, int dataType, long createTime)
            throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));

        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            deletor.delete();
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                    "Failed to delete data", e);
        }
    }

    @Override
    public void deleteData(String wsName, String dataId, int dataType, long createTime, int siteId)
            throws ScmServerException {
        if (siteId == ScmContentModule.getInstance().getLocalSite()) {
            deleteDataLocal(wsName, dataId, dataType, createTime);
            return;
        }

        if (siteId != ScmContentModule.getInstance().getMainSite()) {
            throw new ScmSystemException(
                    "only support delete local site or root site data: site=" + siteId);
        }

        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));
        ScmInnerRemoteDataDeletor deleter = new ScmInnerRemoteDataDeletor(siteId, wsInfo, dataInfo);
        deleter.delete();
    }
    @Override
    public DatasourceReaderDao readData(String workspaceName, String dataId, int dataType,
                                        long createTime, int readflag, OutputStream os) throws ScmServerException {
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        // readflag no need process in current version

        return new DatasourceReaderDao(wsInfo, dataInfo);
    }

    @Override
    public BSONObject getDataInfo(String workspaceName, String dataId, int dataType,
                                  long createTime) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(workspaceName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentModule.getInstance().getDataService(),
                    dataInfo);
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to create data reader", e);
        }
        try {
            long size = reader.getSize();
            BSONObject retInfo = new BasicBSONObject();
            retInfo.put(CommonDefine.RestArg.DATASOURCE_DATA_SIZE, size);
            return retInfo;
        } finally {
            reader.close();
        }
    }

    @Override
    public void createDataInLocal(String wsName, String dataId, int dataType, long createTime) throws ScmServerException {
        createDataInLocal(wsName, dataId, dataType, createTime, null);
    }

    @Override
    public void createDataInLocal(String wsName, String dataId, int dataType, long createTime,
                                  InputStream is) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));

        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        ScmDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createWriter(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), contentModule.getDataService(), dataInfo);
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }
        try {
            if (is != null) {
                while (true) {
                    int actLen = is.read(buf, 0, Const.TRANSMISSION_LEN);
                    if (actLen == -1) {
                        break;
                    }
                    writer.write(buf, 0, actLen);
                }
            }
            writer.close();
        } catch (ScmDatasourceException e) {
            writer.cancel();
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to write data", e);
        } catch (IOException e) {
            writer.cancel();
            throw new ScmServerException(ScmError.NETWORK_IO,
                    "create data failed:wsName=" + wsName + ",dataInfo=" + dataInfo, e);
        } catch (Exception e) {
            writer.cancel();
            throw new ScmSystemException(
                    "create data failed:wsName=" + wsName + ",dataInfo=" + dataInfo, e);
        } finally {
            FileCommonOperator.recordDataTableName(wsName, writer);
        }
    }

    @Override
    public ScmDataInfoDetail createData(String ws, InputStream data, long createTimeMill) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo workspace = contentModule.getWorkspaceInfo(ws);
        if (workspace == null) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST, "workspace not found:" + ws);
        }

        Date createTime = new Date(createTimeMill);
        String dataId = ScmIdGenerator.FileId.get(createTime);

        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), dataId, createTime);
        ScmDataInfoDetail scmDataInfoDetail = new ScmDataInfoDetail(dataInfo);

        if (workspace.getLocationObj(contentModule.getLocalSite()) != null) {
            InputStreamWithCalcMd5 inputStreamWithCalcMd5 = new InputStreamWithCalcMd5(data, false);
            try {
                createDataInLocal(ws, dataId, ENDataType.Normal.getValue(), createTime.getTime(),
                        inputStreamWithCalcMd5);
                scmDataInfoDetail.setMd5(inputStreamWithCalcMd5.calcMd5());
                scmDataInfoDetail.setSize(inputStreamWithCalcMd5.getSize());
            } finally {
                ScmSystemUtils.closeResource(inputStreamWithCalcMd5);
            }
            scmDataInfoDetail.setSiteId(ScmContentModule.getInstance().getLocalSite());
            return scmDataInfoDetail;
        }

        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        ScmInnerRemoteDataWriter writer = new ScmInnerRemoteDataWriter(contentModule.getMainSite(),
                workspace, dataInfo);
        InputStreamWithCalcMd5 inputStreamWithCalcMd5 = null;
        try {
            inputStreamWithCalcMd5 = new InputStreamWithCalcMd5(data, false);
            while (true) {
                int ret = CommonHelper.readAsMuchAsPossible(inputStreamWithCalcMd5, buf);
                if (ret <= -1) {
                    break;
                }
                writer.write(buf, 0, ret);
                if (ret < buf.length) {
                    break;
                }
            }
            writer.close();
            scmDataInfoDetail.setMd5(inputStreamWithCalcMd5.calcMd5());
            scmDataInfoDetail.setSize(inputStreamWithCalcMd5.getSize());
            scmDataInfoDetail.setSiteId(contentModule.getMainSite());
            return scmDataInfoDetail;
        } catch (ScmServerException e) {
            writer.cancel();
            throw e;
        } catch (Exception e) {
            writer.cancel();
            throw new ScmServerException(ScmError.DATA_WRITE_ERROR,
                    "failed to write data to main site: ws=" + ws + ", dataId=" + dataId, e);
        } finally {
            ScmSystemUtils.closeResource(inputStreamWithCalcMd5);
        }
    }

    @Override
    public void deleteDataTables(List<String> tableNames) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmDataTableDeletor deletor;
        try {
            deletor = ScmDataOpFactoryAssit.getFactory().createDataTableDeletor(tableNames,
                    contentModule.getDataService());
            deletor.delete();
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to delete data tables:" + tableNames, e);
        }
    }

    @Override
    public ScmDataReader getScmDataReader(String wsName, String dataId, int dataType,
            long createTime) throws ScmServerException, ScmDatasourceException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));

        ScmDataReader dataReader = null;
        try {
            dataReader = ScmDataOpFactoryAssit.getFactory().createReader(
                    contentModule.getLocalSite(), wsName, wsInfo.getDataLocation(),
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
            int dataType, long createTime) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(wsName);

        ScmSeekableDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createSeekableDataWriter(
                    wsInfo.getDataLocation(), contentModule.getDataService(), wsName, null, dataId,
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
            List<Integer> siteList) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(type, dataId, new Date(createTime));
        ScmFileDataDeleter fileDataDeleter = new ScmFileDataDeleter(siteList, wsInfo, dataInfo);
        fileDataDeleter.deleteData();
    }
}
