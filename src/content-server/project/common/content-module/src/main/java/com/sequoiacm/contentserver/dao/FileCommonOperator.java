package com.sequoiacm.contentserver.dao;

import java.security.MessageDigest;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.remote.ContentServerFeignExceptionConverter;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataReader;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionUtils;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.remote.DataInfo;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataDeletor;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataReader;
import com.sequoiacm.contentserver.remote.ScmInnerRemoteDataWriter;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;

public class FileCommonOperator {
    private static final Logger logger = LoggerFactory.getLogger(FileCommonOperator.class);

    public static boolean isDataExist(ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo, long size) {
        ScmDataReader reader = null;
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
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
                dataInfo.getType(), dataInfo.getCreateTime().getTime(), dataInfo.getWsVersion(),
                dataInfo.getTableName());
        return headDataInfo.getSize();
    }

    public static boolean deleteLocalResidulFile(ScmWorkspaceInfo wsInfo, int localSiteId,
            ScmDataInfo dataInfo) {
        logger.warn("local site exist residul file content:localSiteId={},wsName={},dataId={}",
                localSiteId, wsInfo.getName(), dataInfo.getId());
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
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

    // 将远程站点的数据读到本地再计算 md5，旧接口，只做兼容使用
    public static boolean isRemoteDataExistV1(int remoteSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo, String expectMd5) throws ScmServerException {
        return _isRemoteDataExist(remoteSiteId, wsInfo, dataInfo, expectMd5, true);
    }

    // 让远程站点计算 md5 并返回，新接口
    public static boolean isRemoteDataExistV2(int remoteSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo, String expectMd5) throws ScmServerException {
        return _isRemoteDataExist(remoteSiteId, wsInfo, dataInfo, expectMd5, false);
    }

    // 兼容两种方式，先走新接口，新接口不存在时走旧接口
    public static boolean isRemoteDataExist(int remoteSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo, String expectMd5) throws ScmServerException {
        try {
            return isRemoteDataExistV2(remoteSiteId, wsInfo, dataInfo, expectMd5);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.OPERATION_UNSUPPORTED) {
                return isRemoteDataExistV1(remoteSiteId, wsInfo, dataInfo, expectMd5);
            }
            else {
                throw e;
            }
        }
    }

    private static boolean _isRemoteDataExist(int remoteSiteId, ScmWorkspaceInfo wsInfo,
            ScmDataInfo dataInfo, String expectMd5, boolean localCalc) throws ScmServerException {
        try {
            String remoteDataMd5 = null;
            if (localCalc) {
                remoteDataMd5 = calRemoteDataMd5Local(remoteSiteId, wsInfo.getName(), dataInfo);
            }
            else {
                remoteDataMd5 = calRemoteDataMd5(remoteSiteId, wsInfo.getName(), dataInfo);
            }
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

    public static String calRemoteDataMd5Local(int targetSiteId, String wsName,
            ScmDataInfo dataInfo) throws ScmServerException {
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

        ScmSite siteInfo = contentModule.getSiteInfo(remoteSiteId);
        if (siteInfo == null) {
            throw new ScmServerException(ScmError.SITE_NOT_EXIST,
                    "site not exist:siteId=" + remoteSiteId);
        }
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByServiceName(siteInfo.getName());
        BSONObject res = null;
        try {
            res = client.calcDataMd5KeepAlive(dataInfo.getId(), targetSiteId, wsName,
                    dataInfo.getType(), dataInfo.getCreateTime().getTime(), dataInfo.getWsVersion(),
                    dataInfo.getTableName());
        }
        catch (ScmServerException e) {
            // 旧版本会抛出 403 状态码的异常
            if (e.getError() == ScmError.HTTP_METHOD_NOT_ALLOWED) {
                throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                        "failed to calc data md5 with KeepAlive, the version of the remote node may be too old",
                        e);
            }
            throw e;
        }
        try {
            ScmFeignExceptionUtils.handleException(res);
        }
        catch (ScmFeignException e) {
            throw new ContentServerFeignExceptionConverter().convert(e);
        }
        return BsonUtils.getStringChecked(res, FieldName.FIELD_CLFILE_FILE_MD5);
    }

    public static void addSiteInfoToList(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int addedSiteId, int wsVersion, String tableName)
            throws ScmServerException {
        logger.info("add site to site list:wsName=" + wsInfo.getName() + ",fileId=" + fileId
                + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                + addedSiteId);
        ScmContentModule.getInstance().getMetaService().addSiteInfoToFile(wsInfo, fileId,
                majorVersion, minorVersion, addedSiteId, new Date(), wsVersion, tableName);

    }

    public static void updateAccessTimeInFile(ScmWorkspaceInfo wsInfo, String fileId,
            int majorVersion, int minorVersion, int siteId, Date date) throws ScmServerException {
        logger.debug("updating access time:wsName=" + wsInfo.getName() + ",fileId=" + fileId
                + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                + siteId);
        ScmContentModule.getInstance().getMetaService().updateAccessTimeInFile(wsInfo, fileId,
                majorVersion, minorVersion, siteId, date);

    }

    public static void updateAccessHistoryInFile(ScmWorkspaceInfo wsInfo, String fileId,
            int majorVersion, int minorVersion, int siteId, BasicBSONList newAccessTimeList)
            throws ScmServerException {
        logger.debug("updating access history:wsName=" + wsInfo.getName() + ",fileId=" + fileId
                + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                + siteId);
        ScmContentModule.getInstance().getMetaService().updateAccessHistoryInFile(wsInfo, fileId,
                majorVersion, minorVersion, siteId, newAccessTimeList);

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
