package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataDeletor;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataWriter;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;

public class FileTransferDao {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferDao.class);

    private byte[] readBuff = new byte[Const.TRANSMISSION_LEN];

    private ScmWorkspaceInfo wsInfo;
    private int localSiteId;
    private int remoteSiteId;
    private String fileId;

    private FileTransferInterrupter interrupter = null;

    public FileTransferDao(ScmWorkspaceInfo wsInfo, int targetSiteId) {
        this(wsInfo, targetSiteId, null);
    }

    public FileTransferDao(ScmWorkspaceInfo wsInfo, int remoteSiteId,
            FileTransferInterrupter interrupter) {
        this.wsInfo = wsInfo;
        this.remoteSiteId = remoteSiteId;
        this.interrupter = interrupter;
        this.localSiteId = ScmContentServer.getInstance().getLocalSite();
    }

    // return true if transfer success,return false if transfer get interrupt
    public boolean doTransfer(BSONObject file) throws ScmServerException {
        fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);

        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        int majorVersion = (int) file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) file.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        ScmDataInfo dataInfo = new ScmDataInfo(file);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);

        if (CommonHelper.isSiteExist(remoteSiteId, siteList)) {
            // target site is already exist
            logger.warn("file is already exist in target site:fileId={},target={},version={}",
                    fileId, remoteSiteId, ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            return true;
        }

        if (!CommonHelper.isSiteExist(localSiteId, siteList)) {
            throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                    "file is not exist in source site:fileId=" + fileId + ",source=" + localSiteId
                            + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }
        try {
            // interrupt,return false
            if (!readLocalWriteRomete(dataInfo, majorVersion, minorVersion)) {
                return false;
            }
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.DATA_EXIST) {
                if (FileCommonOperator.isRemoteDataExist(remoteSiteId, wsInfo, dataInfo, size)) {
                    // if data is valid and update meta success,just return
                    FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                            remoteSiteId);
                    return true;
                }
                else {
                    if (!FileCommonOperator.deleteRemoteResidulFile(wsInfo, remoteSiteId, dataInfo)) {
                        logger.error(
                                "transfer file failed:wsName={},fileId={},localSiteId={},targetSiteId={},version={}",
                                wsInfo.getName(), fileId, localSiteId, remoteSiteId,
                                ScmSystemUtils.getVersionStr(majorVersion, minorVersion),e);
                        throw e;
                    }
                    // interrupt,return false
                    try {
                        if (!readLocalWriteRomete(dataInfo, majorVersion, minorVersion)) {
                            return false;
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

        // update meta data info
        FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                remoteSiteId);
        logger.info("add site info success:wsName={},fileId={},addedSiteId={},version={}",
                wsInfo.getName(), fileId, remoteSiteId,
                ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        return true;
    }

    private boolean readLocalWriteRomete(ScmDataInfo dataInfo, int majorVersion, int minorVersion)
            throws ScmServerException {
        ScmInnerRemoteDataWriter writer = null;
        ScmDataReader reader = null;
        try {
            writer = new ScmInnerRemoteDataWriter(remoteSiteId, wsInfo, dataInfo);
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
            int length = 0;
            resetInterrupter();
            while ((length = reader.read(readBuff, 0, Const.TRANSMISSION_LEN)) != -1) {
                writer.write(readBuff, 0, length);
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
