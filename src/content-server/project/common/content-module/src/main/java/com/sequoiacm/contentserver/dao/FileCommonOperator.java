package com.sequoiacm.contentserver.dao;

import java.security.MessageDigest;
import java.util.Date;

import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataReader;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.remote.DataInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataDeletor;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataWriter;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;

import javax.xml.bind.DatatypeConverter;

public class FileCommonOperator {
    private static final Logger logger = LoggerFactory.getLogger(FileCommonOperator.class);

    public static boolean isDataExist(ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo, long size) {
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            if (size == reader.getSize()) {
                return true;
            }
        }
        catch (Exception e) {
            // ignore this operation
            logger.warn("open data failed:dataInfo=" + dataInfo, e);
        }
        finally {
            FileCommonOperator.closeReader(reader);
        }

        return false;
    }

    public static long getSize(String remoteSiteName, String wsName, ScmDataInfo dataInfo)
            throws ScmServerException {
        ContentServerClient client = null;
        ScmContentModule contentModule = ScmContentModule.getInstance();
        if (ScmStrategyMgr.getInstance().strategyType() == StrategyType.STAR && !contentModule.isInMainSite()) {
            String mainSiteName = ScmContentModule.getInstance().getMainSiteName();
            client = ContentServerClientFactory.getFeignClientByServiceName(mainSiteName);
        }
        else {
            client = ContentServerClientFactory.getFeignClientByServiceName(remoteSiteName);
        }
        DataInfo headDataInfo = client.headDataInfo(remoteSiteName, wsName, dataInfo.getId(),
                dataInfo.getType(), dataInfo.getCreateTime().getTime());
        return headDataInfo.getSize();
    }

    public static boolean deleteLocalResidulFile(ScmWorkspaceInfo wsInfo, int localSiteId,
            ScmDataInfo dataInfo) {
        logger.warn("local site exist residul file content:localSiteId={},wsName={},dataId={}",
                localSiteId, wsInfo.getName(), dataInfo.getId());
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            deletor.delete();
            logger.info("delete residul file content success:localSiteId={},wsName={},dataId={}",
                    localSiteId, wsInfo.getName(), dataInfo.getId());
        }
        catch (Exception e) {
            logger.error("delete residul file content failed:localSiteId={},wsName={},dataId={}",
                    localSiteId, wsInfo.getName(), dataInfo.getId(), e);
            return false;
        }
        return true;
    }

    public static boolean deleteRemoteResidulFile(ScmWorkspaceInfo wsInfo, int remoteSiteId,
            ScmDataInfo dataInfo) {
        logger.warn("remote site exist residul file content:remoteSiteId={},wsName={},dataId={}",
                remoteSiteId, wsInfo.getName(), dataInfo.getId());
        try {
            ScmInnerRemoteDataDeletor deletor = new ScmInnerRemoteDataDeletor(remoteSiteId, wsInfo,
                    dataInfo);
            deletor.delete();
            logger.info("delete residul file content success:remoteSiteId={},wsName={},dataId={}",
                    remoteSiteId, wsInfo.getName(), dataInfo.getId());
        }
        catch (ScmServerException e) {
            logger.error("delete residul file content failed:remoteSiteId={},wsName={},dataId={}",
                    remoteSiteId, wsInfo.getName(), dataInfo.getId(), e);
            return false;
        }
        return true;
    }

    public static boolean isRemoteDataExist(int remoteSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo, long size) throws ScmServerException {
        try {
            String remoteSiteName = ScmContentModule.getInstance().getSiteInfo(remoteSiteId)
                    .getName();
            long remoteDataSize = getSize(remoteSiteName, wsInfo.getName(), dataInfo);
            if (size == remoteDataSize) {
                return true;
            }
            logger.warn("remote data size is not right:remoteSite=" + remoteSiteId + "ws="
                    + wsInfo.getName() + ",dataId=" + dataInfo.getId() + ",remoteDataSize="
                    + remoteDataSize + ",expectDataSize=" + size);
            return false;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.DATA_NOT_EXIST) {
                return false;
            }
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("check remote data failed:remoteSite=" + remoteSiteId
                    + ",ws=" + wsInfo.getName() + ",dataId=" + dataInfo.getId(), e);
        }
    }

    public static boolean isRemoteDataExist(int remoteSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo, String expectMd5) throws ScmServerException {
        try {
            String remoteDataMd5 = calRemoteDataMd5(remoteSiteId, wsInfo.getName(), dataInfo);
            if (remoteDataMd5.equals(expectMd5)) {
                return true;
            }
            logger.warn("remote data md5 is not right:remoteSite=" + remoteSiteId + "ws="
                    + wsInfo.getName() + ",dataId=" + dataInfo.getId() + ",remoteDataMd5="
                    + remoteDataMd5 + ",expectDataMd5=" + expectMd5);
            return false;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.DATA_NOT_EXIST) {
                return false;
            }
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("check remote data failed:remoteSite=" + remoteSiteId
                    + ",ws=" + wsInfo.getName() + ",dataId=" + dataInfo.getId(), e);
        }
    }

    public static String calRemoteDataMd5(int targetSiteId, String wsName, ScmDataInfo dataInfo)
            throws ScmServerException {
        int remoteSiteId;
        ScmContentModule contentModule = ScmContentModule.getInstance();
        if (ScmStrategyMgr.getInstance().strategyType() == StrategyType.STAR
                && !contentModule.isInMainSite()) {
            remoteSiteId = contentModule.getMainSite();
        }
        else {
            remoteSiteId = targetSiteId;
        }
        ScmInnerRemoteDataReader remoteDataReader = null;
        try {
            remoteDataReader = new ScmInnerRemoteDataReader(remoteSiteId,
                    contentModule.getWorkspaceInfoCheckExist(wsName), dataInfo, 0, targetSiteId);
            MessageDigest md5Calc = ScmSystemUtils.createMd5Calc();
            byte[] buf = new byte[Const.TRANSMISSION_LEN];
            while (true) {
                int len = remoteDataReader.read(buf, 0, Const.TRANSMISSION_LEN);
                if (len <= -1) {
                    break;
                }
                md5Calc.update(buf, 0, len);
            }
            return DatatypeConverter.printBase64Binary(md5Calc.digest());
        }
        catch (Exception e) {
            logger.error("failed to calculate remote data md5, targetSiteId={}, dataId={}",
                    targetSiteId, dataInfo.getId());
            throw e;
        }
        finally {
            if (remoteDataReader != null) {
                remoteDataReader.close();
            }
        }

    }

    public static void addSiteInfoToList(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int addedSiteId) throws ScmServerException {
        logger.info("add site to site list:wsName=" + wsInfo.getName() + ",fileId=" + fileId
                + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                + addedSiteId);
        ScmContentModule.getInstance().getMetaService().addSiteInfoToFile(wsInfo, fileId,
                majorVersion, minorVersion, addedSiteId, new Date());

    }

    public static void updateAccessTimeInFile(ScmWorkspaceInfo wsInfo, String fileId,
            int majorVersion, int minorVersion, int siteId, Date date) throws ScmServerException {
        logger.debug("updating access time:wsName=" + wsInfo.getName() + ",fileId=" + fileId
                + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                + siteId);
        ScmContentModule.getInstance().getMetaService().updateAccessTimeInFile(wsInfo, fileId,
                majorVersion, minorVersion, siteId, date);

    }

    public static void deleteSiteFromFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int siteId) throws ScmServerException {
        logger.info("delete site from file:wsName=" + wsInfo.getName() + ",fileId=" + fileId
                + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                + siteId);
        ScmContentModule.getInstance().getMetaService().deleteSiteFromFile(wsInfo, fileId,
                majorVersion, minorVersion, siteId);
    }

    public static void closeReader(ScmDataReader reader) {
        if (null != reader) {
            reader.close();
        }
    }

    public static void cancelWriter(ScmDataWriter writer) {
        if (null != writer) {
            writer.cancel();
        }
    }

    public static void closeWriter(ScmDataWriter writer) throws ScmServerException {
        if (null != writer) {
            try {
                writer.close();
            }
            catch (ScmDatasourceException e) {
                throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                        "Failed to close data writer", e);
            }
        }
    }

    public static void closeRemoteWriter(ScmInnerRemoteDataWriter writer)
            throws ScmServerException {
        if (null != writer) {
            writer.close();
        }
    }

    public static void cancelRemoteWriter(ScmInnerRemoteDataWriter writer) {
        if (null != writer) {
            writer.cancel();
        }
    }

    public static void recordDataTableName(String wsName, ScmDataWriter writer) {
        if (writer != null && writer.getCreatedTableName() != null) {
            recordTableName(wsName, writer.getCreatedTableName());
        }
    }

    public static void recordDataTableName(String wsName, ScmBreakpointDataWriter writer) {
        if (writer != null && writer.getCreatedTableName() != null) {
            recordTableName(wsName, writer.getCreatedTableName());
        }
    }

    private static void recordTableName(String wsName, String tableName) {
        try {
            ScmContentModule.getInstance().getMetaService().recordDataTableName(wsName, tableName);
        }
        catch (Exception e) {
            logger.warn("failed to record data table name:wsName={},tableName={}", wsName,
                    tableName);
        }
    }
}
