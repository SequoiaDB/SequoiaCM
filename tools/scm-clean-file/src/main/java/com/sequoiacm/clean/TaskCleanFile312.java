package com.sequoiacm.clean;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockPathDefine;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.DataInfo;
import com.sequoiacm.contentserver.remote.RemoteCommonUtil;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionConverter;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.sequoiadb.dataopertion.SdbDataOpFactoryImpl;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

enum DoFileRes {
    SUCCESS,
    FAIL,
    SKIP
}

public class TaskCleanFile312 implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TaskCleanFile312.class);
    private final RemoteClientMgr remoteClientMgr;

    private DBCollection metaFileCl;
    private final SequoiadbDatasource metaSdbDs;
    private String ws;
    private com.sequoiacm.infrastructure.lock.ScmLockManager lockManager;
    private String cleanSiteName;
    private int cleanSiteId;
    private SdbDataOpFactoryImpl sdbDataOpFactory;
    private SdbDataLocation cleanSiteDataLocation;
    private SdbDataService cleanSiteDataService;
    private Sequoiadb metaSdb;

    private ScmFeignUtil feignUtil;
    private SiteInfoMgr siteInfoMgr;
    private FileCounter fileCounter;
    private String fileId;
    private int major;
    private int minor;
    private String dataId;

    public TaskCleanFile312(SequoiadbDatasource metaSdbDs, String ws, ScmLockManager lockManager,
            String cleanSiteName, int cleanSiteId, SdbDataOpFactoryImpl sdbDataOpFactory,
            SdbDataLocation cleanSiteDataLocation, SdbDataService cleanSiteDataService,
            ScmFeignUtil feignUtil, SiteInfoMgr siteInfoMgr, FileCounter fileCounter, String fileId,
            int major, int minor, String dataId, RemoteClientMgr mgr) throws InterruptedException {
        this.metaSdbDs = metaSdbDs;
        this.ws = ws;
        this.lockManager = lockManager;
        this.cleanSiteName = cleanSiteName;
        this.cleanSiteId = cleanSiteId;
        this.sdbDataOpFactory = sdbDataOpFactory;
        this.cleanSiteDataLocation = cleanSiteDataLocation;
        this.cleanSiteDataService = cleanSiteDataService;
        this.feignUtil = feignUtil;
        this.siteInfoMgr = siteInfoMgr;
        this.fileCounter = fileCounter;
        this.fileId = fileId;
        this.major = major;
        this.minor = minor;
        this.dataId = dataId;
        this.remoteClientMgr = mgr;
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
                        cleanSiteName, ScmLockPathDefine.FILE_CONTENT, dataId }));
    }

    private boolean isRemoteDataExist(int dataInOtherSiteId, ScmDataInfo dataInfo, long size)
            throws ScmServerException {
        String url = siteInfoMgr.getHoldingDataSiteInstanceUrl(dataInOtherSiteId);
        ContentServerClient client = remoteClientMgr.getClient(url);
        try {
            DataInfo a = client.headDataInfo(siteInfoMgr.getSitNameById(dataInOtherSiteId), ws,
                    dataInfo.getId(), dataInfo.getType(), dataInfo.getCreateTime().getTime());
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

    private DoFileRes cleanFile(String fileId, int majorVersion, int minorVersion)
            throws ScmServerException, SdbMetasourceException, InterruptedException,
            StrategyException {
        BSONObject file = getFileInfo(fileId, majorVersion, minorVersion);
        if (file == null) {
            logger.warn("skip, file is not exist:fileId={},version={}.{}", fileId, majorVersion,
                    minorVersion);
            return DoFileRes.SKIP;
        }
        long size = (long) file.get(FieldName.FIELD_CLFILE_FILE_SIZE);
        ScmDataInfo dataInfo = new ScmDataInfo(file);
        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);
        List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);
        if (!siteIdList.contains(cleanSiteId)) {
            logger.info("file not exist in local site:fileId={},version={}.{}", fileId,
                    majorVersion, minorVersion);
            return DoFileRes.SUCCESS;
        }

        List<Integer> verifySites = NetworkStrategy.getCleanTaskVerifySites(
                siteInfoMgr.getWsLocationSiteId(), siteIdList, cleanSiteId);

        // List<Integer> verifySites = ScmStrategyMgr.getInstance().getVerifySites(ws,
        // siteIdList,
        // localSite);
        if (verifySites.size() == 0) {
            logger.warn("the file exists only at local site:workspace=" + ws + ",fileId=" + fileId);
            return DoFileRes.SKIP;
        }

        for (int site : verifySites) {

            if (isRemoteDataExist(site, dataInfo, size)) {
                // if (FileCommonOperator.isRemoteDataExist(site, ws, dataInfo, size)) {
                // if occur exception here,just throw it,caller will process
                deleteSiteFromFile(fileId, majorVersion, minorVersion);
                // FileCommonOperator.deleteSiteFromFile(ws, fileId, majorVersion, minorVersion,
                // getLocalSiteId());
                try {
                    ScmDataDeletor deleter = sdbDataOpFactory.createDeletor(cleanSiteId, ws,
                            cleanSiteDataLocation, cleanSiteDataService, dataInfo);
                    // ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    // ScmContentServer.getInstance().getLocalSite(), ws.getName(),
                    // ws.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    // dataInfo);
                    deleter.delete();
                }
                catch (ScmDatasourceException e) {
                    throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                            "Failed to delete file", e);
                }
                return DoFileRes.SUCCESS;
                // not need to close deletor
            }
            else {
                logger.warn("file data is not exist in the site:siteId=" + site + ",fileId="
                        + fileId + ",workspace=" + ws);
            }
        }
        return DoFileRes.SKIP;
    }

    protected DoFileRes doFile(String fileId, int majorVersion, int minorVersion, String dataId)
            throws ScmServerException, ScmLockException, SdbMetasourceException,
            InterruptedException, StrategyException {
        // ScmLockPath fileContentLockPath =
        // ScmLockPathFactory.createFileContentLockPath(
        // getWorkspaceInfo().getName(),
        // ScmContentServer.getInstance().getLocalSiteInfo().getName(), dataId);
        // ScmLock fileContentLock =
        // ScmLockManager.getInstance().tryAcquiresLock(fileContentLockPath);
        ScmLock fileContentLock = tryLockFileContent(cleanSiteId, dataId);
        if (fileContentLock == null) {
            logger.warn("try lock failed, skip this file:fileId={},version={}.{},dataId={}", fileId,
                    majorVersion, minorVersion, dataId);
            return DoFileRes.SKIP;
        }

        try {
            return cleanFile(fileId, majorVersion, minorVersion);
        }
        catch (ScmServerException e) {
            // skip exception
            if (e.getError() == ScmError.DATA_TYPE_ERROR || e.getError() == ScmError.FILE_NOT_FOUND
                    || e.getError() == ScmError.DATA_NOT_EXIST) {
                logger.warn("clean file failed:fileId={},version={}.{},dataId={}", fileId,
                        majorVersion, minorVersion, dataId, e);
                return DoFileRes.SKIP;
            }

            // failed exception
            if (e.getError() == ScmError.DATA_UNAVAILABLE
                    || e.getError() == ScmError.DATA_CORRUPTED) {
                logger.warn("clean file failed:fileId={},version={}.{},dataId={}", fileId,
                        majorVersion, minorVersion, dataId, e);
                return DoFileRes.FAIL;
            }

            // abort exception
            throw e;
        }
        finally {
            fileContentLock.unlock();
        }
    }

    @Override
    public void run() {
        ScmLock lock = null;
        try {
            metaSdb = metaSdbDs.getConnection();
            metaFileCl = metaSdb.getCollectionSpace(ws + "_META").getCollection("FILE");
            lock = lockManager.acquiresReadLock(new ScmLockPath(new String[] {
                    ScmLockPathDefine.WORKSPACES, ws, ScmLockPathDefine.FILES, fileId }));

            DoFileRes res = doFile(fileId, major, minor, dataId);
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
            logger.error("fatal error, process file failed: ws={}, fileid={}, minor={}, major={}",
                    ws, fileId, minor, major, e);
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
