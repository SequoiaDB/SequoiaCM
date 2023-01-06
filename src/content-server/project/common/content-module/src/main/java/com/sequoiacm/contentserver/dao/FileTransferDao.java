package com.sequoiacm.contentserver.dao;

import java.security.MessageDigest;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataWriter;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class FileTransferDao {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferDao.class);

    private byte[] readBuff = new byte[Const.TRANSMISSION_LEN];

    private ScmWorkspaceInfo wsInfo;
    private int localSiteId;
    private int remoteSiteId;
    private String fileId;
    private String localDataMd5;

    private FileTransferInterrupter interrupter = null;
    private String dataCheckLevel = CommonDefine.DataCheckLevel.WEEK;

    public enum FileTransferResult {
        SUCCESS,
        INTERRUPT,
        DATA_INCORRECT
    }

    public FileTransferDao(ScmWorkspaceInfo wsInfo, int targetSiteId) {
        this(wsInfo, targetSiteId, null, CommonDefine.DataCheckLevel.WEEK);
    }

    public FileTransferDao(ScmWorkspaceInfo wsInfo, int remoteSiteId,
            FileTransferInterrupter interrupter, String dataCheckLevel) {
        this.wsInfo = wsInfo;
        this.remoteSiteId = remoteSiteId;
        this.interrupter = interrupter;
        this.localSiteId = ScmContentModule.getInstance().getLocalSite();
        this.dataCheckLevel = dataCheckLevel;
        this.localSiteId = ScmContentModule.getInstance().getLocalSite();
    }

    public FileTransferResult doTransfer(BSONObject file) throws ScmServerException {
        fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);
        localDataMd5 = (String) file.get(FieldName.FIELD_CLFILE_FILE_MD5);

        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        int majorVersion = (int) file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) file.get(FieldName.FIELD_CLFILE_MINOR_VERSION);

        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(sites);
        ScmFileLocation localFileLocation = fileLocationMap.get(localSiteId);
        if (localFileLocation == null) {
            throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                    "file is not exist in source site:fileId=" + fileId + ",source=" + localSiteId
                            + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        ScmDataInfo localDataInfo = ScmDataInfo.forOpenExistData(file,
                localFileLocation.getWsVersion(),
                localFileLocation.getTableName());
        if (fileLocationMap.get(remoteSiteId) != null) {
            logger.warn("file is already exist in target site:fileId={},target={},version={}",
                    fileId, remoteSiteId, ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            // target site is already exist
            ScmDataInfo targetDataInfo = ScmDataInfo.forOpenExistData(file,
                    fileLocationMap.get(remoteSiteId).getWsVersion(),
                    fileLocationMap.get(remoteSiteId).getTableName());
            return checkDataIsSame(localDataInfo, targetDataInfo, size, majorVersion, minorVersion)
                    ? FileTransferResult.SUCCESS
                    : FileTransferResult.DATA_INCORRECT;
        }


        ScmDataInfo remoteDataInfo = ScmDataInfo.forCreateNewData(file, wsInfo.getVersion());
        ScmDataWriterContext remoteWriterContext = new ScmDataWriterContext();
        try {
            // interrupt,return false
            if (!readLocalWriteRomete(localDataInfo, remoteDataInfo, majorVersion, minorVersion,
                    remoteWriterContext)) {
                return FileTransferResult.INTERRUPT;
            }
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.DATA_EXIST) {
                String tableName = null;
                if (e.getExtraInfo() != null) {
                    tableName = (String) e.getExtraInfo()
                            .get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME);
                }
                remoteDataInfo.setTableName(tableName);
                remoteWriterContext.recordTableName(tableName);
                if (checkDataIsSame(localDataInfo, remoteDataInfo, size, majorVersion, minorVersion)) {
                    // if data is valid and update meta success,just return
                    FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                            remoteSiteId, remoteDataInfo.getWsVersion(), remoteWriterContext);
                    return FileTransferResult.SUCCESS;
                }
                else {
                    if (!FileCommonOperator.deleteRemoteResidulFile(wsInfo, remoteSiteId, remoteDataInfo)) {
                        logger.error(
                                "transfer file failed:wsName={},fileId={},localSiteId={},targetSiteId={},version={}",
                                wsInfo.getName(), fileId, localSiteId, remoteSiteId,
                                ScmSystemUtils.getVersionStr(majorVersion, minorVersion),e);
                        throw e;
                    }
                    // interrupt,return false
                    try {
                        if (!readLocalWriteRomete(localDataInfo, remoteDataInfo, majorVersion,
                                minorVersion, remoteWriterContext)) {
                            return FileTransferResult.INTERRUPT;
                        }
                    }
                    catch (ScmServerException e1) {
                        logger.error(
                                "transfer file failed:wsName={},fileId={},localSiteId={},targetSiteId={},version={}",
                                wsInfo.getName(), fileId, localSiteId, remoteSiteId,
                                ScmSystemUtils.getVersionStr(majorVersion, minorVersion),e);
                        throw e1;
                    }
                }
            }
            else {
                logger.error(
                        "transfer file failed:wsName={},fileId={},localSiteId={},targetSiteId={},version={}",
                        wsInfo.getName(), fileId, localSiteId, remoteSiteId,
                        ScmSystemUtils.getVersionStr(majorVersion, minorVersion),e);
                throw e;
            }
        }

        remoteDataInfo.setTableName(remoteWriterContext.getTableName());
        if (!checkDataIsSame(localDataInfo, remoteDataInfo, size, majorVersion, minorVersion)) {
            FileCommonOperator.deleteRemoteResidulFile(wsInfo, remoteSiteId, remoteDataInfo);
            return FileTransferResult.DATA_INCORRECT;
        }
        // update meta data info
        FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                remoteSiteId, remoteDataInfo.getWsVersion(), remoteWriterContext);
        return FileTransferResult.SUCCESS;
    }

    private boolean checkDataIsSame(ScmDataInfo localDataInfo, ScmDataInfo remoteDataInfo, long dataSize, int majorVersion,
            int minorVersion) {
        try {
            if (CommonDefine.DataCheckLevel.STRICT.equals(dataCheckLevel)) {
                String localDataMd5 = null;
                try {
                    localDataMd5 = this.localDataMd5 != null ? this.localDataMd5
                            : ScmSystemUtils.calcMd5(wsInfo, localDataInfo);
                }
                catch (Exception e) {
                    logger.warn("failed to calculate local data md5,fileId={},siteId={},version={}",
                            fileId, localSiteId,
                            ScmSystemUtils.getVersionStr(majorVersion, minorVersion), e);
                    return false;
                }
                return FileCommonOperator.isRemoteDataExist(remoteSiteId, wsInfo, remoteDataInfo,
                        localDataMd5);
            }
            else {
                return FileCommonOperator.isRemoteDataExist(remoteSiteId, wsInfo, remoteDataInfo,
                        dataSize);
            }
        }
        catch (Exception e) {
            logger.warn(
                    "failed to check file data, localSite={},remoteSite={},fileId={},version={}",
                    localSiteId, remoteSiteId, fileId,
                    ScmSystemUtils.getVersionStr(majorVersion, minorVersion), e);
            return false;
        }
    }

    private boolean readLocalWriteRomete(ScmDataInfo localDataInfo, ScmDataInfo remoteDataInfo,
            int majorVersion, int minorVersion, ScmDataWriterContext remoteWriterContext)
            throws ScmServerException {
        ScmInnerRemoteDataWriter writer = null;
        ScmDataReader reader = null;
        MessageDigest md5Calc = null;
        if (CommonDefine.DataCheckLevel.STRICT.equals(dataCheckLevel)) {
            md5Calc = ScmSystemUtils.createMd5Calc();
            this.localDataMd5 = null;
        }
        try {
            writer = new ScmInnerRemoteDataWriter(remoteSiteId, wsInfo, remoteDataInfo,
                    remoteWriterContext);
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(localDataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
                    localDataInfo);
            int length = 0;
            resetInterrupter();
            while ((length = reader.read(readBuff, 0, Const.TRANSMISSION_LEN)) != -1) {
                writer.write(readBuff, 0, length);
                if (md5Calc != null) {
                    md5Calc.update(readBuff, 0, length);
                }
                if (isInterrupter(length)) {
                    logger.info(
                            "transfer get interrupt:wsName={},localSite={},remoteSite={},fileId={},version={}",
                            wsInfo.getName(), localSiteId, remoteSiteId, fileId,
                            ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
                    FileCommonOperator.cancelRemoteWriter(writer);
                    FileCommonOperator.closeReader(reader);
                    // interrupt,return false
                    return false;
                }
            }

            FileCommonOperator.closeReader(reader);
            reader = null;
            FileCommonOperator.closeRemoteWriter(writer);
            writer = null;
            if (md5Calc != null) {
                this.localDataMd5 = DatatypeConverter.printBase64Binary(md5Calc.digest());
            }
        }
        catch (ScmServerException e) {
            FileCommonOperator.cancelRemoteWriter(writer);
            FileCommonOperator.closeReader(reader);
            throw e;
        }
        catch (ScmDatasourceException e) {
            FileCommonOperator.cancelRemoteWriter(writer);
            FileCommonOperator.closeReader(reader);
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "failed to transfer file", e);
        }
        return true;
    }

    private void resetInterrupter() {
        if (null != interrupter) {
            interrupter.resetLen();
        }
    }

    private boolean isInterrupter(int length) {
        if (null != interrupter) {
            return interrupter.isInterrupted(length);
        }
        return false;
    }
}
