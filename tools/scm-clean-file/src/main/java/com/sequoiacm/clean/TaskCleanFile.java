package com.sequoiacm.clean;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockPathDefine;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.DataInfo;
import com.sequoiacm.contentserver.remote.RemoteCommonUtil;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;

import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

enum DoFileRes {
    SUCCESS,
    FAIL,
    SKIP
}

public class TaskCleanFile implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TaskCleanFile.class);
    private final RemoteClientMgr remoteClientMgr;
    private final Entry.FileScopeEnum fileScope;

    private DBCollection metaFileCl;
    private final SequoiadbDatasource metaSdbDs;
    private String ws;
    private ScmLockManager lockManager;
    private int cleanSiteId;
    private int holdingDataSiteId;
    private ScmDataOpFactory scmDataOpFactory;
    private ScmLocation cleanSiteDataLocation;
    private ScmService cleanSiteDataService;
    private Sequoiadb metaSdb;
    private SiteInfoMgr siteInfoMgr;
    private FileCounter fileCounter;
    private BSONObject fileInfoNotInLock;
    private ThreadPoolExecutor threadPool;
    private ScmShutDownHook shutDownHook;

    public TaskCleanFile(SequoiadbDatasource metaSdbDs, String ws, ScmLockManager lockManager,
            int cleanSiteId, int holdingDataSiteId, ScmDataOpFactory scmDataOpFactory,
            ScmLocation cleanSiteDataLocation, ScmService cleanSiteDataService,
            SiteInfoMgr siteInfoMgr, FileCounter fileCounter, BSONObject fileInfoNotInLock,
            RemoteClientMgr mgr, ThreadPoolExecutor threadPool, ScmShutDownHook shutDownHook,
            Entry.FileScopeEnum fileScope) {
        this.metaSdbDs = metaSdbDs;
        this.ws = ws;
        this.lockManager = lockManager;
        this.cleanSiteId = cleanSiteId;
        this.holdingDataSiteId = holdingDataSiteId;
        this.scmDataOpFactory = scmDataOpFactory;
        this.cleanSiteDataLocation = cleanSiteDataLocation;
        this.cleanSiteDataService = cleanSiteDataService;
        this.siteInfoMgr = siteInfoMgr;
        this.fileCounter = fileCounter;
        this.fileInfoNotInLock = fileInfoNotInLock;
        this.remoteClientMgr = mgr;
        this.threadPool = threadPool;
        this.shutDownHook = shutDownHook;
        this.fileScope = fileScope;
    }

    public void destroy() {
        if (metaSdb != null) {
            metaSdbDs.releaseConnection(metaSdb);
        }
    }

    private BSONObject getFileInfo(String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        BSONObject currentFileMatcher = new BasicBSONObject();
        currentFileMatcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
        currentFileMatcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
        ScmMetaSourceHelper.addFileIdAndCreateMonth(currentFileMatcher, fileId);
        return metaFileCl.queryOne(currentFileMatcher, null, null, null, 0);
    }

    private ScmLock tryLockFileContent(int siteId, String dataId)
            throws ScmServerException, ScmLockException {
        return lockManager.tryAcquiresLock(new com.sequoiacm.infrastructure.lock.ScmLockPath(
                new String[] { ScmLockPathDefine.WORKSPACES, ws, ScmLockPathDefine.SITES,
                        siteInfoMgr.getSiteNameById(siteId), ScmLockPathDefine.FILE_CONTENT,
                        dataId }));
    }

    private void unlock(ScmLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

    private boolean isRemoteDataExist(int dataInOtherSiteId, ScmDataInfo dataInfo, long size)
            throws ScmServerException {
        String url = siteInfoMgr.getHoldingDataSiteInstanceUrl(dataInOtherSiteId);
        ContentServerClient client = remoteClientMgr.getClient(url);
        try {
            DataInfo a = client.headDataInfo(siteInfoMgr.getSiteNameById(dataInOtherSiteId), ws,
                    dataInfo.getId(), dataInfo.getType(), dataInfo.getCreateTime().getTime(),
                    dataInfo.getWsVersion(), dataInfo.getTableName());

            if (size == a.getSize()) {
                return true;
            }
            logger.warn("remote data size is not right:remoteSite=" + dataInOtherSiteId + "ws=" + ws
                    + ",dataId=" + dataInfo.getId() + ",remoteDataSize=" + a.getSize()
                    + ",expectDataSize=" + size);
            return false;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.DATA_NOT_EXIST) {
                return false;
            }
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("check remote data failed:remoteSite=" + dataInOtherSiteId
                    + ",ws=" + ws + ",dataId=" + dataInfo.getId(), e);
        }
    }

    private void deleteSiteFromFile(String fileId, int majorVersion, int minorVersion)
            throws SdbMetasourceException, InterruptedException {
        BSONObject matcher = SequoiadbHelper.dollarSiteInList(cleanSiteId);
        // matcher.put(FieldName.FIELD_CLFILE_ID, fileId);
        SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
        matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
        matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
        metaFileCl.update(matcher, SequoiadbHelper.unsetDollar0FromList(), null);

        BSONObject pullNulMatcher = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(pullNulMatcher, fileId);
        metaFileCl.update(pullNulMatcher, SequoiadbHelper.pullNullFromList(), null);
    }

    private DoFileRes cleanFile(String fileId, int majorVersion, int minorVersion,
            int dataInOtherSiteId)
            throws ScmServerException, SdbMetasourceException, InterruptedException {
        BSONObject file = getFileInfo(fileId, majorVersion, minorVersion);
        if (file == null) {
            logger.warn("skip, file is not exist:fileId={},version={}.{}", fileId, majorVersion,
                    minorVersion);
            return DoFileRes.SKIP;
        }

        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
//        ScmDataInfo dataInfo = new ScmDataInfo(file);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(sites);

        if (fileLocationMap.get(cleanSiteId) == null) {
            logger.warn(
                    "skip, file data is not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    ws, fileId, majorVersion, minorVersion, fileLocationMap);
            return DoFileRes.SKIP;
        }

        if (fileLocationMap.get(dataInOtherSiteId) == null) {
            // 锁外检查，文件在本站点和一个远端站点（dataInOtherSiteId），锁住本站点和远端站点后，发现文件已不存在于远端站点，安全起见放弃本站点的文件清理
            logger.warn(
                    "skip, file data is not in locking remote site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}, lockingRemoteSite={}",
                    ws, fileId, majorVersion, minorVersion, fileLocationMap, dataInOtherSiteId);
            return DoFileRes.SKIP;
        }
        // 确认对端站点数据确实可用(规避极端情况下，数据损坏等问题)，再删除本地站点数据
        ScmFileLocation location = fileLocationMap.get(dataInOtherSiteId);
        ScmDataInfo remoteDataInfo = ScmDataInfo.forOpenExistData(file, location.getWsVersion(),
                location.getTableName());
        if (isRemoteDataExist(dataInOtherSiteId, remoteDataInfo, size)) {
            deleteSiteFromFile(fileId, majorVersion, minorVersion);
            try {
                ScmDataInfo cleanDataInfo = ScmDataInfo.forOpenExistData(file,
                        fileLocationMap.get(cleanSiteId).getWsVersion(),
                        fileLocationMap.get(cleanSiteId).getTableName());
                ScmDataDeletor deleter = scmDataOpFactory.createDeletor(cleanSiteId, ws,
                        cleanSiteDataLocation, cleanSiteDataService, cleanDataInfo);
                deleter.delete();
            }
            catch (ScmDatasourceException e) {
                ScmError scmError = e.getScmError(ScmError.DATA_DELETE_ERROR);
                if (scmError == ScmError.FILE_NOT_FOUND || scmError == ScmError.DATA_NOT_EXIST
                        || scmError == ScmError.DATA_IS_IN_USE
                        || scmError == ScmError.DATA_UNAVAILABLE) {
                    logger.warn("metasource updated successfully, but failed to delete data:fileId="
                            + fileId + ",workspace=" + ws, e);
                    return DoFileRes.SUCCESS;
                }
                else {
                    throw new ScmServerException(scmError, "failed to delete file", e);
                }
            }
            return DoFileRes.SUCCESS;
        }
        logger.warn("file data is not exist in the remote locking site:siteId=" + dataInOtherSiteId
                + ",fileId=" + fileId + ",workspace=" + ws);
        return DoFileRes.SKIP;
    }

    protected DoFileRes doFile(BSONObject fileInfoNotInLock)
            throws ScmServerException, ScmLockException, SdbMetasourceException,
            InterruptedException {
        String fileId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_ID);
        String dataId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        int majorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MINOR_VERSION);

        BasicBSONList siteList = BsonUtils.getArrayChecked(fileInfoNotInLock,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<Integer> fileDataSiteIdList = CommonHelper.getFileLocationIdList(siteList);
        if (!fileDataSiteIdList.contains(cleanSiteId)) {
            logger.warn(
                    "skip, file data not in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    ws, fileId, majorVersion, minorVersion, fileDataSiteIdList);
            return DoFileRes.SKIP;
        }

        if (fileDataSiteIdList.size() < 2) {
            logger.warn(
                    "skip, file data only in local site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    ws, fileId, majorVersion, minorVersion, fileDataSiteIdList);
            return DoFileRes.SKIP;
        }

        if (!fileDataSiteIdList.contains(holdingDataSiteId)) {
            logger.warn(
                    "skip, file data is not in remote site: workspace={}, fileId={}, version={}.{}, fileDataSiteList={}",
                    ws, fileId, majorVersion, minorVersion, fileDataSiteIdList);
            return DoFileRes.SKIP;
        }

        ScmLock localFileContentLock = null;
        ScmLock otherSiteFileContentLock = null;
        try {
            localFileContentLock = tryLockFileContent(cleanSiteId, dataId);
            if (localFileContentLock == null) {
                logger.warn(
                        "try lock local data failed, skip this file: workspace={}, fileId={},version={}.{}, dataId={}",
                        ws, fileId, majorVersion, minorVersion, dataId);
                return DoFileRes.SKIP;
            }
            otherSiteFileContentLock = tryLockFileContent(holdingDataSiteId, dataId);
            if (otherSiteFileContentLock == null) {
                logger.warn(
                        "try lock remote data failed, skip this file: workspace={}, fileId={},version={}.{},dataId={}",
                        ws, fileId, majorVersion, minorVersion, dataId);
                return DoFileRes.SKIP;
            }
            return cleanFile(fileId, majorVersion, minorVersion, holdingDataSiteId);
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("skip, clean file failed: workspace={}, fileId={}, version={}.{}", ws,
                        fileId, majorVersion, minorVersion, e);
                return DoFileRes.SKIP;
            }

            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE
                    || e.getError() == ScmError.DATA_CORRUPTED) {
                logger.warn("clean file failed: workspace={}, fileId={}, version={}.{}", ws, fileId,
                        majorVersion, minorVersion, e);
                return DoFileRes.FAIL;
            }

            // abort exception
            throw e;
        }
        finally {
            unlock(otherSiteFileContentLock);
            unlock(localFileContentLock);
        }
    }

    @Override
    public void run() {
        if (shutDownHook.isShutdown()) {
            return;
        }
        ScmLock lock = null;
        String fileId = (String) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_ID);
        int majorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = (int) fileInfoNotInLock.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
        try {
            metaSdb = metaSdbDs.getConnection();
            metaFileCl = metaSdb.getCollectionSpace(ws + "_META").getCollection(
                    fileScope == Entry.FileScopeEnum.HISTORY ? "FILE_HISTORY" : "FILE");
            lock = lockManager.acquiresReadLock(new ScmLockPath(new String[] {
                    ScmLockPathDefine.WORKSPACES, ws, ScmLockPathDefine.FILES, fileId }));

            DoFileRes res = doFile(fileInfoNotInLock);
            if (res == DoFileRes.SUCCESS) {
                fileCounter.getSuccess().incrementAndGet();
            }
            else if (res == DoFileRes.FAIL) {
                fileCounter.getFailed().incrementAndGet();
            }
            else {
                fileCounter.getSkip().incrementAndGet();
            }
            fileCounter.logCounter();
        }
        catch (Throwable e) {
            logger.error(
                    "fatal error, process file failed: ws={}, fileId={}, majorVersion={}, minorVersion={}",
                    ws, fileId, majorVersion, minorVersion, e);
            fileCounter.getFailed().incrementAndGet();
            fileCounter.logCounter();
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
            destroy();
        }
    }
}

class DataInfoDecoder implements Decoder {
    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object decode(Response response, Type type) throws IOException, DecodeException {
        if (!DataInfo.class.equals(type)) {
            throw new DecodeException("Invalid type: " + type.toString());
        }

        String dataInfoStr = RemoteCommonUtil.firstOrNull(response.headers(),
                CommonDefine.RestArg.DATASOURCE_DATA_HEADER);
        if (dataInfoStr == null) {
            throw new DecodeException("Failed to decode data info, missing header:header="
                    + CommonDefine.RestArg.DATASOURCE_DATA_HEADER);
        }
        return mapper.readValue(dataInfoStr, DataInfo.class);
    }
}

class ContentServerFeignExceptionConverter
        implements ScmFeignExceptionConverter<ScmServerException> {
    @Override
    public ScmServerException convert(ScmFeignException e) {
        return new ScmServerException(ScmError.getScmError(e.getStatus()), e.getMessage(), e);
    }
}
