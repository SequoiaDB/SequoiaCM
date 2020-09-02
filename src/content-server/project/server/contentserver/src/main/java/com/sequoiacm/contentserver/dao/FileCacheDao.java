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
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataReader;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;

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
        this.localSiteId = ScmContentServer.getInstance().getLocalSite();
    }

    public void doCache(BSONObject file) throws ScmServerException {
        fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);

        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        int majorVersion = (int) file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) file.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        ScmDataInfo dataInfo = new ScmDataInfo(file);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);

        if (CommonHelper.isSiteExist(localSiteId, siteList)) {
            // target site is already exist
            logger.warn(
                    "file is already exist in local site:wsName={},fileId={},siteId={},version={}",
                    wsInfo.getName(), fileId, localSiteId,
                    ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            return;
        }

        if (!CommonHelper.isSiteExist(remoteSiteId, siteList)) {
            throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                    "file is not exist in remote site:wsName=" + wsInfo.getName() + ",fileId="
                            + fileId + ",siteId=" + remoteSiteId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        ScmDataWriter writer = null;
        ScmInnerRemoteDataReader reader = null;
        try {
            try {
                writer = createLocalWriter(dataInfo);
            }
            catch (ScmServerException e) {
                if (e.getError() == ScmError.DATA_EXIST) {
                    if (FileCommonOperator.isDataExist(wsInfo, dataInfo, size)) {
                        // update meta data info and return
                        FileCommonOperator.addSiteInfoToList(wsInfo, fileId, majorVersion,
                                minorVersion, localSiteId);
                        return;
                    }
                    if (!FileCommonOperator.deleteLocalResidulFile(wsInfo, localSiteId, dataInfo)) {
                        throw e;
                    }
                    writer = createLocalWriter(dataInfo);
                }
                else {
                    throw e;
                }
            }
            reader = new ScmInnerRemoteDataReader(remoteSiteId, wsInfo, dataInfo, 0);
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
                localSiteId);
        logger.info("add site info success:wsName={},fileId={},addedSiteId={},version={}",
                wsInfo.getName(), fileId, localSiteId,
                ScmSystemUtils.getVersionStr(majorVersion, minorVersion));

    }

    private ScmDataWriter createLocalWriter(ScmDataInfo dataInfo) throws ScmServerException {
        ScmDataWriter writer = null;
        try {
            writer = ScmDataOpFactoryAssit.getFactory().createWriter(localSiteId, wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "failed to create data writer");
        }
        return writer;
    }

}
