package com.sequoiacm.contentserver.dao;

import java.util.Map;

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
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataReader;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class FileCacheDao {
    private static final Logger logger = LoggerFactory.getLogger(FileCacheDao.class);

    private byte[] readBuff = new byte[Const.TRANSMISSION_LEN];

    private ScmWorkspaceInfo wsInfo;
    private int localSiteId;
    private int remoteSiteId;
    private String fileId;

    public FileCacheDao(ScmWorkspaceInfo wsInfo, int remoteSiteId) {
        this.wsInfo = wsInfo;
        this.remoteSiteId = remoteSiteId;
        this.localSiteId = ScmContentModule.getInstance().getLocalSite();
    }

    public void doCache(BSONObject file) throws ScmServerException {
        fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);

        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        int majorVersion = (int) file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) file.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(sites);

        if (fileLocationMap.get(localSiteId) != null) {
            // target site is already exist
            logger.warn(
                    "file is already exist in local site:wsName={},fileId={},siteId={},version={}",
                    wsInfo.getName(), fileId, localSiteId,
                    ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            return;
        }
        ScmFileLocation remoteLocation = fileLocationMap.get(remoteSiteId);
        if (remoteLocation == null) {
            throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                    "file is not exist in remote site:wsName=" + wsInfo.getName() + ",fileId="
                            + fileId + ",siteId=" + remoteSiteId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        ScmDataInfo localDataInfo = ScmDataInfo.forCreateNewData(file, wsInfo.getVersion());
        ScmDataInfo remoteDataInfo = ScmDataInfo.forOpenExistData(file,
                remoteLocation.getWsVersion(),
                remoteLocation.getTableName());

        ScmDataWriter writer = null;
        ScmInnerRemoteDataReader reader = null;
        ScmDataWriterContext localDataWriterContext = new ScmDataWriterContext();
        try {
            try {
                writer = createLocalWriter(localDataInfo, localDataWriterContext);
            }
            catch (ScmServerException e) {
                if (e.getError() == ScmError.DATA_EXIST) {
                    localDataInfo.setTableName(localDataWriterContext.getTableName());
                    if (FileCommonOperator.isDataExist(wsInfo, localDataInfo, size)) {
                        // update meta data info and return
                        FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion,
                                minorVersion, localSiteId, localDataInfo.getWsVersion(),
                                localDataWriterContext);
                        return;
                    }
                    if (!FileCommonOperator.deleteLocalResidulFile(wsInfo, localSiteId, localDataInfo)) {
                        throw e;
                    }

                    writer = createLocalWriter(localDataInfo, localDataWriterContext);
                }
                else {
                    throw e;
                }
            }
            reader = new ScmInnerRemoteDataReader(remoteSiteId, wsInfo, remoteDataInfo, 0);
            int length = 0;
            while ((length = reader.read(readBuff, 0, Const.TRANSMISSION_LEN)) != -1) {
                try {
                    writer.write(readBuff, 0, length);
                }
                catch (ScmDatasourceException e) {
                    throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                            "failed to write data", e);
                }
            }
            reader.close();
            reader = null;
            FileCommonOperator.closeWriter(writer);
            writer = null;
        }
        catch (Exception e) {
            logger.error(
                    "cache file failed:wsName={},fileId={},localSiteId={},remoteSiteId={},version={}",
                    wsInfo.getName(), fileId, localSiteId, remoteSiteId,
                    ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            if (reader != null) {
                reader.close();
            }
            FileCommonOperator.cancelWriter(writer);
            throw e;
        }
        finally {
            FileCommonOperator.recordDataTableName(wsInfo.getName(), writer);
        }

        // update meta data info
        FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion, minorVersion,
                localSiteId, localDataInfo.getWsVersion(), localDataWriterContext);
        logger.info("add site info success:wsName={},fileId={},addedSiteId={},version={}",
                wsInfo.getName(), fileId, localSiteId,
                ScmSystemUtils.getVersionStr(majorVersion, minorVersion));

    }

    private ScmDataWriter createLocalWriter(ScmDataInfo dataInfo, ScmDataWriterContext context)
            throws ScmServerException {
        ScmDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createWriter(localSiteId, wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
                    dataInfo, context);
        }
        catch (ScmDatasourceException e) {
            logger.error("create data writer fail:id={},type={},createTime={}", dataInfo.getId(), dataInfo.getType(), dataInfo.getCreateTime());
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "failed to create data writer", e);
        }
        return writer;
    }

}
