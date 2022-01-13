package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.InputStreamWithCalcMd5;
import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataWriter;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.site.ScmContentServer;
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
    public void deleteData(String wsName, String dataId, int dataType, long createTime)
            throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));

        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                    "Failed to delete data", e);
        }
    }

    @Override
    public DatasourceReaderDao readData(String workspaceName, String dataId, int dataType,
            long createTime, int readflag, OutputStream os) throws ScmServerException {
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(workspaceName);

        // readflag no need process in current version

        return new DatasourceReaderDao(wsInfo, dataInfo);
    }

    @Override
    public BSONObject getDataInfo(String workspaceName, String dataId, int dataType,
            long createTime) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentServer.getInstance()
                .getWorkspaceInfoChecked(workspaceName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to create data reader", e);
        }
        try {
            long size = reader.getSize();
            BSONObject retInfo = new BasicBSONObject();
            retInfo.put(CommonDefine.RestArg.DATASOURCE_DATA_SIZE, size);
            return retInfo;
        }
        finally {
            reader.close();
        }
    }

    @Override
    public void createDataInLocal(String wsName, String dataId, int dataType, long createTime,
            InputStream is) throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(wsName);
        ScmDataInfo dataInfo = new ScmDataInfo(dataType, dataId, new Date(createTime));

        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        ScmDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createWriter(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), contentserver.getDataService(), dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }
        try {
            while (true) {
                int actLen = is.read(buf, 0, Const.TRANSMISSION_LEN);
                if (actLen == -1) {
                    break;
                }
                writer.write(buf, 0, actLen);
            }
            writer.close();
            logger.info("inner write file finished:ws=" + wsName + ",dataInfo=" + dataInfo);
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
    public ScmDataInfoDetail createData(String ws, InputStream data) throws ScmServerException {
        InputStreamWithCalcMd5 inputStreamWithCalcMd5 = new InputStreamWithCalcMd5(data);
        ScmContentServer cs = ScmContentServer.getInstance();
        ScmWorkspaceInfo workspace = cs.getWorkspaceInfo(ws);
        if (workspace == null) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST, "workspace not found:" + ws);
        }

        Date createTime = new Date();
        String dataId = ScmIdGenerator.FileId.get(createTime);

        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), dataId, createTime);
        ScmDataInfoDetail scmDataInfoDetail = new ScmDataInfoDetail(dataInfo);

        if (workspace.getLocationObj(cs.getLocalSite()) != null) {
            createDataInLocal(ws, dataId, ENDataType.Normal.getValue(), createTime.getTime(),
                    inputStreamWithCalcMd5);
            scmDataInfoDetail.setMd5(inputStreamWithCalcMd5.calcMd5());
            scmDataInfoDetail.setSize(inputStreamWithCalcMd5.getSize());
            scmDataInfoDetail.setSiteId(ScmContentServer.getInstance().getLocalSite());
            return scmDataInfoDetail;
        }

        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        ScmInnerRemoteDataWriter writer = new ScmInnerRemoteDataWriter(cs.getMainSite(), workspace,
                dataInfo);
        try {
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
        }
        catch (ScmServerException e) {
            writer.cancel();
            throw e;
        }
        catch (Exception e) {
            writer.cancel();
            throw new ScmServerException(ScmError.DATA_WRITE_ERROR,
                    "failed to write data to main site: ws=" + ws + ", dataId=" + dataId, e);
        }
        scmDataInfoDetail.setMd5(inputStreamWithCalcMd5.calcMd5());
        scmDataInfoDetail.setSize(inputStreamWithCalcMd5.getSize());
        scmDataInfoDetail.setSiteId(cs.getMainSite());
        return scmDataInfoDetail;
    }

    @Override
    public void deleteDataTables(List<String> tableNames) throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmDataTableDeletor deletor;
        try {
            deletor = ScmDataOpFactoryAssit.getFactory().createDataTableDeletor(tableNames,
                    contentserver.getDataService());
            deletor.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_ERROR),
                    "Failed to delete data tables:" + tableNames, e);
        }
    }

}
