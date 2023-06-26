package com.sequoiacm.transfer;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class FileTransfer {
    private static final Logger logger = LoggerFactory.getLogger(FileTransfer.class);
    private final SiteInfoMgr siteInfoMgr;
    private final int batchSize;
    private final ScmTimer timer;
    private final Sequoiadb sdbConnForCheckTask;
    private final DBCollection sdbFileClForCheckTask;
    private final ConfigOptions sdbConnOps;
    private final List<String> sdbCoord;
    private final String sdbUser;
    private final String sdbPassword;
    private final Entry.FileScopeEnum scopeEnum;
    private final String fileClName;
    private long fileTimeout;
    String wsName;
    private BSONObject matcher;
    List<String> urlList;
    String user;
    String password;
    private int targetSiteId;
    private volatile long successCount = 0;
    private volatile long timeoutCount = 0;
    private long lastLoggerCountTime = 0;
    private Semaphore semp;
    private ConcurrentHashMap<String, FileInfoWrapper> submitFiles;

    private int fileStatusCheckBatchSize = 100;


    public FileTransfer(int batchSize, long fileTimeout, int thread, int queueSize,
            String srcSiteName, String targetSiteName, final String wsName, BSONObject matcher,
                        final List<String> urlList, final String user, final String password,
            int fileStatusCheckInterval, List<String> sdbCoord, String sdbUser, String sdbPassword,
            int fileStatusCheckBatchSize, ConfigOptions sdbConnOps, Entry.FileScopeEnum scopeEnum)
            throws ScmMappingException {
        this.wsName = wsName;
        this.fileTimeout = fileTimeout;
        this.matcher = matcher;
        this.urlList = urlList;
        this.user = user;
        this.password = password;
        this.batchSize = batchSize;
        this.semp = new Semaphore(batchSize);
        submitFiles = new ConcurrentHashMap(batchSize);
        this.fileStatusCheckBatchSize = fileStatusCheckBatchSize;
        this.sdbConnOps = sdbConnOps;
        this.sdbCoord = sdbCoord;
        this.sdbUser = sdbUser;
        this.sdbPassword = sdbPassword;
        this.scopeEnum = scopeEnum;
        if (scopeEnum == Entry.FileScopeEnum.ALL) {
            throw new IllegalArgumentException("file transfer only support "
                    + Entry.FileScopeEnum.HISTORY + " or " + Entry.FileScopeEnum.CURRENT);
        }
        this.fileClName = scopeEnum == Entry.FileScopeEnum.HISTORY ? "FILE_HISTORY" : "FILE";

        // 后台任务使用的连接句柄
        sdbConnForCheckTask = new Sequoiadb(sdbCoord, sdbUser, sdbPassword, sdbConnOps);
        sdbFileClForCheckTask = sdbConnForCheckTask.getCollectionSpace(wsName + "_META")
                .getCollection(fileClName);

        this.siteInfoMgr = new SiteInfoMgr(sdbConnForCheckTask);
        targetSiteId = siteInfoMgr.getSiteIdByName(targetSiteName);

        BasicBSONList andArr = new BasicBSONList();

        // 用户条件
        andArr.add(matcher);

        // 目标站点不存在该文件
        BasicBSONList notList = new BasicBSONList();
        notList.add(new BasicBSONObject("site_list.$0.site_id", targetSiteId));
        andArr.add(new BasicBSONObject("$not", notList));

        // 源站点存在该文件
        andArr.add(new BasicBSONObject("site_list.$0.site_id",
                siteInfoMgr.getSiteIdByName(srcSiteName)));

        this.matcher = new BasicBSONObject("$and", andArr);

        timer = ScmTimerFactory.createScmTimer();
        timer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    checkCompleteFile();
                } catch (Throwable e) {
                    logger.error("fatal error, check file status task exit !", e);
                }
            }
        }, fileStatusCheckInterval, fileStatusCheckInterval);
    }

    public void destroy() {
        if (timer != null) {
            try {
                boolean isExit = timer.cancelAndAwaitTermination(30000);
                if (!isExit) {
                    logger.warn("failed to wait background task exit, cause by timeout");
                }
            }
            catch (InterruptedException e) {
                logger.warn("failed to wait background task exit", e);
            }
        }
        if (sdbConnForCheckTask != null) {
            sdbConnForCheckTask.close();
        }
        logger.info("{} process finish: successTransferCount={}, timeoutCount={}", scopeEnum,
                successCount, timeoutCount);
    }

    public void transfer() throws ScmException, InterruptedException {
        Sequoiadb sdbConnForListFile = new Sequoiadb(sdbCoord, sdbUser, sdbPassword, sdbConnOps);
        DBCursor cursor = null;
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(urlList, user, password));
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(wsName, session);

            DBCollection sdbFileClForListFile = sdbConnForListFile
                    .getCollectionSpace(wsName + "_META").getCollection(fileClName);
            logger.info("start to query file record: fileCl={}, matcher={}",
                    sdbFileClForListFile.getFullName(), matcher);
            cursor = sdbFileClForListFile.query(matcher, null, null, null);
            while (cursor.hasNext()) {
                semp.acquire();
                BSONObject fileBSON = cursor.getNext();
                ScmFileBasicInfo file = new ScmFileBasicInfo(fileBSON);
                try {
                    ScmFactory.File.asyncCache(workspace, file.getFileId(), file.getMajorVersion(),
                            file.getMinorVersion());
                    submitFiles.put(file.getFileId().get(), new FileInfoWrapper(file));
                } catch (ScmException e) {
                    if (e.getError() != ScmError.FILE_NOT_FOUND) {
                        throw e;
                    }
                }
            }
        } finally {
            IOUtils.close(session, cursor, sdbConnForListFile);
        }
        semp.acquire(batchSize);
    }

    private void loggerProcessCount(long now) {
        if (now - lastLoggerCountTime > 1000 * 60) {
            logger.info("{} file process info: successTransferCount={}, timeoutCount={}", scopeEnum,
                    successCount, timeoutCount);
            lastLoggerCountTime = now;
        }
    }

    void checkCompleteFile() throws ScmException {
        long now = System.currentTimeMillis();
        BasicBSONList ids = new BasicBSONList();
        BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLFILE_ID, new BasicBSONObject("$in", ids));
        BSONObject selector = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SITE_LIST, null);
        selector.put(FieldName.FIELD_CLFILE_ID, null);
        for (FileInfoWrapper fileWrapper : submitFiles.values()) {
            ScmFileBasicInfo file = fileWrapper.getFileBasicInfo();
            if (fileWrapper.isTimeout(now, fileTimeout)) {
                semp.release();
                timeoutCount++;
                submitFiles.remove(file.getFileId().get());
                logger.warn("check the file status timeout: ws={}, file={}, version={}.{}", wsName, file.getFileId(), file.getMajorVersion(), file.getMinorVersion());
                continue;
            }
            ids.add(file.getFileId().get());
            if (ids.size() >= fileStatusCheckBatchSize) {
                checkCompleteFileByMatcher(matcher, selector);
                ids.clear();
            }
        }
        if (!ids.isEmpty()) {
            checkCompleteFileByMatcher(matcher, selector);
            ids.clear();
        }
        loggerProcessCount(now);
    }

    private void checkCompleteFileByMatcher(BSONObject matcher, BSONObject selector) {
        DBCursor cursor = sdbFileClForCheckTask.query(matcher, selector, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                BasicBSONList siteList = BsonUtils.getArrayChecked(record, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
                if (isContainTargetSite(siteList)) {
                    semp.release();
                    submitFiles.remove(BsonUtils.getStringChecked(record, FieldName.FIELD_CLFILE_ID));
                    successCount++;
                }
            }
        } catch (Exception e) {
            logger.warn("failed to check the file status: ws={}, fileIds{}", wsName, matcher, e);
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
    }

    private boolean isContainTargetSite(BasicBSONList siteList) {
        for (Object location : siteList) {
            BSONObject site = (BSONObject) location;
            int siteId = BsonUtils.getNumberChecked(site, FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID).intValue();
            if (siteId == targetSiteId) {
                return true;
            }
        }
        return false;
    }

}

class FileInfoWrapper {
    private ScmFileBasicInfo fileBasicInfo;
    private long startTime;

    public FileInfoWrapper(ScmFileBasicInfo fileBasicInfo) {
        this.fileBasicInfo = fileBasicInfo;
        this.startTime = System.currentTimeMillis();
    }

    public ScmFileBasicInfo getFileBasicInfo() {
        return fileBasicInfo;
    }

    public boolean isTimeout(long now, long timeout) {
        if (now - startTime > timeout) {
            return true;
        }
        return false;
    }
}


