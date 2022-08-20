package com.sequoiacm.transfer;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
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
    private final int batchSize;
    private final ScmTimer timer;
    private final Sequoiadb sdb;
    private final DBCollection sdbFileCl;
    private long fileTimeout;
    String wsName;
    private BSONObject matcher;
    private ScmType.ScopeType scope;
    List<String> urlList;
    String user;
    String password;
    private int localSiteId;
    private volatile long successCount = 0;
    private volatile long timeoutCount = 0;
    private long lastLoggerCountTime = 0;
    private Semaphore semp;
    private ConcurrentHashMap<String, FileInfoWrapper> submitFiles;

    private int fileStatusCheckBatchSize = 100;


    public FileTransfer(int batchSize, long fileTimeout, int localSiteId,
                        final String wsName, BSONObject matcher,
                        final List<String> urlList, final String user, final String password,
                        int fileStatusCheckInterval, String sdbCoord, String sdbUser, String sdbPassword, int fileStatusCheckBatchSize) throws ScmException {
        this.wsName = wsName;
        this.fileTimeout = fileTimeout;
        this.localSiteId = localSiteId;
        this.matcher = matcher;
        this.scope = ScmType.ScopeType.SCOPE_CURRENT;
        this.urlList = urlList;
        this.user = user;
        this.password = password;
        this.batchSize = batchSize;
        this.semp = new Semaphore(batchSize);
        submitFiles = new ConcurrentHashMap(batchSize);
        this.fileStatusCheckBatchSize = fileStatusCheckBatchSize;

        sdb = new Sequoiadb(sdbCoord, sdbUser, sdbPassword);
        sdbFileCl = sdb.getCollectionSpace(wsName + "_META").getCollection("FILE");

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
            timer.cancel();
        }
        if (sdb != null) {
            sdb.close();
        }
    }

    public void transfer() throws ScmException, InterruptedException {
        ScmCursor<ScmFileBasicInfo> cursor = null;
        ScmSession session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION, new ScmConfigOption(urlList, user, password));
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(wsName, session);
            cursor = ScmFactory.File.listInstance(workspace, scope, matcher);
            while (cursor.hasNext()) {
                semp.acquire();
                ScmFileBasicInfo file = cursor.getNext();
                try {
                    ScmFactory.File.asyncCache(workspace, file.getFileId(), file.getMajorVersion(), file.getMinorVersion());
                    submitFiles.put(file.getFileId().get(), new FileInfoWrapper(file));
                } catch (ScmException e) {
                    if (e.getError() != ScmError.FILE_NOT_FOUND) {
                        throw e;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            session.close();
        }
        semp.acquire(batchSize);
        logger.info("process finish: successTransferCount={}, timeoutCount={}", successCount, timeoutCount);
    }

    private void loggerProcessCount(long now) {
        if (now - lastLoggerCountTime > 1000 * 60) {
            logger.info("process info: successTransferCount={}, timeoutCount={}", successCount, timeoutCount);
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
        DBCursor cursor = sdbFileCl.query(matcher, selector, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                BasicBSONList siteList = BsonUtils.getArrayChecked(record, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
                if (isContainLocalSite(siteList)) {
                    semp.release();
                    submitFiles.remove(BsonUtils.getStringChecked(record, FieldName.FIELD_CLFILE_ID));
                    successCount++;
                }
            }
        } catch (Exception e) {
            logger.warn("failed to check the file status: ws={}, fileIds{}", wsName, matcher, e);
        } finally {
            cursor.close();
        }
    }

    private boolean isContainLocalSite(BasicBSONList siteList) {
        for (Object location : siteList) {
            BSONObject site = (BSONObject) location;
            int siteId = BsonUtils.getNumberChecked(site, FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID).intValue();
            if (siteId == localSiteId) {
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


