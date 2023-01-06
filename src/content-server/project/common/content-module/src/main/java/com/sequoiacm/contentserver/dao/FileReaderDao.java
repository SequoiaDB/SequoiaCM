package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ScmFileReader;
import com.sequoiacm.contentserver.remote.ScmLocalFileReader;
import com.sequoiacm.contentserver.remote.ScmRemoteFileReader;
import com.sequoiacm.contentserver.remote.ScmRemoteFileReaderSeakable;
import com.sequoiacm.contentserver.remote.ScmRemoteFileReaderWrapper;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.strategy.common.StrategyDefine;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;

public class FileReaderDao {
    private static final Logger logger = LoggerFactory.getLogger(FileReaderDao.class);

    ScmFileReader fileReader = null;
    int majorVersion;
    int minorVersion;
    int localSiteId;
    String fileId;
    String wsName;
    ScmWorkspaceInfo wsInfo;
    boolean isNeedSeek = false;

    public FileReaderDao(String sessionId, String userDetail, ScmWorkspaceInfo wsInfo,
            BSONObject fileRecord, int flag) throws ScmServerException {
        this.majorVersion = BsonUtils.getInteger(fileRecord, FieldName.FIELD_CLFILE_MAJOR_VERSION);
        this.minorVersion = BsonUtils.getInteger(fileRecord, FieldName.FIELD_CLFILE_MINOR_VERSION);
        this.fileId = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_ID);
        this.wsInfo = wsInfo;
        this.wsName = wsInfo.getName();

        ScmContentModule contentModule = ScmContentModule.getInstance();
        localSiteId = contentModule.getLocalSite();
        long size = BsonUtils.getLong(fileRecord, FieldName.FIELD_CLFILE_FILE_SIZE);
        BasicBSONList sites = BsonUtils.getArray(fileRecord, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        boolean isExistNull = CommonHelper.getFileLocationList(sites, siteList);
        if (isExistNull) {
            pullNullSite();
        }

        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(sites);

        if (isReadLocalOnly(flag)) {
            ScmFileLocation localFileLocation = fileLocationMap.get(localSiteId);
            if (null == localFileLocation) {
                throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                        "Data is not exist in the site. siteId:" + localSiteId + ", wsName:"
                                + wsName + ", fileId:" + fileId);
            }
            ScmDataInfo localDataInfo = ScmDataInfo.forOpenExistData(fileRecord,
                    localFileLocation.getWsVersion(), localFileLocation.getTableName());
            fileReader = new ScmLocalFileReader(localSiteId, wsInfo, localDataInfo);
            return;
        }

        isNeedSeek = isNeedSeek(flag);
        if (!isNeedSeek && wsInfo.getSiteCacheStrategy() == ScmSiteCacheStrategy.NEVER) {
            // 当工作缓存策略为不缓存时，同时客户端不需要 seek，置位 FORCE_NO_CACHE
            flag |= CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE;
        }

        List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);
        boolean dontCacheLocal = false;
        // read from local
        ScmFileLocation localFileLocation = fileLocationMap.get(localSiteId);
        if (localFileLocation != null) {
            // create local reader
            try {
                ScmDataInfo localDataInfo = ScmDataInfo.forOpenExistData(fileRecord,
                        localFileLocation.getWsVersion(), localFileLocation.getTableName());
                fileReader = createLocalReader(localDataInfo, size);
                return;
            }
            catch (ScmServerException e) {
                String errorMsg = String.format(
                        "read data from local failed:ws=%s,fileId=%s,version=%s.%s",
                        wsInfo.getName(), fileId, majorVersion, minorVersion);
                if (e.getError() != ScmError.DATA_NOT_EXIST) {
                    if (isNeedSeek) {
                        // we need read from local for seek option, throw
                        // exception here.
                        throw new ScmServerException(e.getError(), errorMsg, e);
                    }
                    dontCacheLocal = true;
                }
                logger.warn(errorMsg, e);
            }

            // except local site,because local data is unavailable
            // NOTE: "siteIdList.remove(localSiteId)" is wrong !!!!
            siteIdList.remove(new Integer(localSiteId));
        }

        // read from remote
        if (siteIdList.size() != 0 && isNeedSeek && isForceNoCache(flag)) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "unsupported create seekable remote reader, if dont cache local");
        }

        ScmDataInfo writeLocalDataInfo = ScmDataInfo.forCreateNewData(fileRecord,
                wsInfo.getVersion());
        while (siteIdList.size() != 0) {
            SiteInfo siteInfo = ScmStrategyMgr.getInstance().getNearestSite(wsInfo, siteIdList,
                    localSiteId, fileId);
            int remoteId = siteInfo.getId();

            if (siteInfo.getFlag() == StrategyDefine.SiteType.FLAG_GOTO_SITE) {
                fileReader = createRemoteReader(remoteId, sessionId, userDetail, flag, size,
                        writeLocalDataInfo, dontCacheLocal);
                return;
            }

            try {
                fileReader = createRemoteReader(remoteId, sessionId, userDetail,
                        flag | CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE, size,
                        writeLocalDataInfo, dontCacheLocal);
                return;
            }
            catch (Exception e) {
                logger.warn(
                        "read data from remote failed:remoteId={},ws={},fileId={},version={}.{}",
                        remoteId, wsInfo.getName(), fileId, majorVersion, minorVersion, e);
            }

            // except this remote site.
            // NOTE: "siteIdList.remove(remoteId)" is wrong !!!!
            siteIdList.remove(new Integer(remoteId));
        }
        throw new ScmServerException(ScmError.DATA_ERROR,
                "can not read data from any site:siteList=" + siteList + ",wsName="
                        + wsInfo.getName() + ",fileId=" + writeLocalDataInfo.getId());
    }

    public String getWsName() {
        return wsName;
    }

    public String getFileId() {
        return fileId;
    }

    private ScmFileReader createLocalReader(ScmDataInfo dataInfo, long dataSize)
            throws ScmServerException {
        ScmFileReader localReader = null;
        try {
            localReader = new ScmLocalFileReader(localSiteId, wsInfo, dataInfo);
            long actualSize = localReader.getSize();
            if (actualSize != dataSize) {
                throw new ScmServerException(ScmError.DATA_CORRUPTED,
                        "local data is corrupted:ws=" + wsInfo.getName() + ",fileId=" + fileId
                                + ",version=" + majorVersion + "." + minorVersion + ",expectSize="
                                + dataSize + ",actualSize=" + actualSize);
            }
            return localReader;
        }
        catch (Exception e) {
            if (localReader != null) {
                localReader.close();
            }
            throw e;
        }
    }

    private ScmFileReader createRemoteReader(int targetSiteId, String sessionId, String userDetail,
            int flag, long size, ScmDataInfo dataInfo, boolean dontCacheLocal)
            throws ScmServerException {
        // read data from remote site
        ScmFileReader remoteReader = null;
        try {
            if (isNeedSeek) {
                if (dontCacheLocal) {
                    // should not come here because of 'dontCacheLocal' is
                    // false if need seek.
                    throw new ScmServerException(ScmError.DATA_ERROR,
                            "local datasource is unavailable, failed to create seekable reader:ws="
                                    + wsInfo.getName() + ",localSiteId=" + localSiteId + ",data="
                                    + dataInfo);
                }
                // ScmRemoteFileReader + ScmLocalFileReader
                remoteReader = new ScmRemoteFileReaderSeakable(sessionId, userDetail, localSiteId,
                        targetSiteId, wsInfo, fileId, this.majorVersion, this.minorVersion, size,
                        dataInfo, flag);
            }
            else {
                if (dontCacheLocal || isForceNoCache(flag)) {
                    remoteReader = new ScmRemoteFileReader(sessionId, userDetail, targetSiteId,
                            wsInfo, fileId, majorVersion, minorVersion, flag);
                }
                else {
                    remoteReader = new ScmRemoteFileReaderWrapper(sessionId, userDetail,
                            localSiteId, targetSiteId, wsInfo, fileId, this.majorVersion,
                            this.minorVersion, size, dataInfo, flag);
                }
            }
            long actualSize = remoteReader.getSize();
            if (remoteReader.getSize() != size) {
                throw new ScmServerException(ScmError.DATA_CORRUPTED,
                        "remote data is corrupted:remoteId=" + targetSiteId + ",ws="
                                + wsInfo.getName() + ",fileId=" + fileId + ",version="
                                + majorVersion + "." + minorVersion + ",expectSize=" + size
                                + ",actualSize=" + actualSize);
            }
            return remoteReader;
        }
        catch (Exception e) {
            if (remoteReader != null) {
                remoteReader.close();
            }
            throw e;
        }
    }

    private void pullNullSite() {
        try {
            logger.info("update site info:wsName=" + wsName + ",fileId=" + fileId + ",majorVersion="
                    + majorVersion + ",minorVersion=" + minorVersion);
            ScmContentModule.getInstance().getMetaService().deleteNullSiteFromFile(wsInfo, fileId,
                    majorVersion, minorVersion);
        }
        catch (Exception e) {
            logger.warn("update site info failed:wsName=" + wsName + ",fileId=" + fileId, e);
        }
    }

    private void recordLastAccessTime(String wsName, String fileId, int majorVersion,
            int minorVersion, int localSiteId, Date date) {
        try {
            FileCommonOperator.updateAccessTimeInFile(wsInfo, fileId, majorVersion, minorVersion,
                    localSiteId, date);
        }
        catch (Exception e) {
            logger.warn("record last access time failed:wsName=" + wsName + ",fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion + ",siteId="
                    + localSiteId, e);
        }
    }

    private boolean isForceNoCache(int flag) {
        return (flag & CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE) > 0;
    }

    private boolean isNeedSeek(int flag) {
        if ((flag & CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK) > 0) {
            return true;
        }

        return false;
    }

    private boolean isReadLocalOnly(int flag) {
        if ((flag & CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE) > 0) {
            return true;
        }

        return false;
    }

    public int read(byte[] buff, int offset, int len) throws ScmServerException {
        try {
            return fileReader.read(buff, offset, len);
        }
        catch (Exception e) {
            logger.error("read file failed:fileId=" + fileId + ",offset=" + offset + ",len=" + len);
            throw e;
        }
    }

    public void read(OutputStream os) throws ScmServerException {
        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        while (true) {
            int len = read(buf, 0, Const.TRANSMISSION_LEN);
            if (len <= -1) {
                break;
            }
            try {
                os.write(buf, 0, len);
            }
            catch (IOException e) {
                throw new ScmServerException(ScmError.FILE_IO,
                        "read file failed:ws=" + wsName + ",fileId=" + fileId + ",version="
                                + ScmSystemUtils.getVersionStr(majorVersion, minorVersion),
                        e);
            }
        }
    }

    public boolean isEof() {
        return fileReader.isEof();
    }

    public void close() {
        if (null != fileReader) {
            fileReader.close();
            fileReader = null;
            recordLastAccessTime(wsName, fileId, majorVersion, minorVersion, localSiteId,
                    new Date());
        }
    }

    public void seek(long size) throws ScmServerException {
        if (size == 0) {
            return;
        }

        try {
            fileReader.seek(size);
        }
        catch (Exception e) {
            logger.error("seek file failed:fileId=" + fileId + ",seekSize=" + size);
            throw e;
        }
    }

    public long getSize() throws ScmServerException {
        return fileReader.getSize();
    }

    private boolean isLocalSiteHaveData(int localSite, List<ScmFileLocation> siteList) {
        for (ScmFileLocation i : siteList) {
            if (localSite == i.getSiteId()) {
                // if it is in local, just return local site
                return true;
            }
        }
        return false;
    }
}