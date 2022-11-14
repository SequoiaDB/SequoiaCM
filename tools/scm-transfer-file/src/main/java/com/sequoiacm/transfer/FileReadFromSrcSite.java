package com.sequoiacm.transfer;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ScmSessionWrapper {
    private ScmSession session;
    private ScmWorkspace ws;

    public ScmSessionWrapper(ScmSession session, ScmWorkspace ws) {
        this.session = session;
        this.ws = ws;
    }

    public ScmSession getSession() {
        return session;
    }

    public ScmWorkspace getWs() {
        return ws;
    }
}

public class FileReadFromSrcSite {
    private static final Logger logger = LoggerFactory.getLogger(FileTransfer.class);
    private SiteInfoMgr siteInfoMgr;
    private final int batchSize;
    private final ScmTimer timer;
    private final Sequoiadb sdb;
    private final DBCollection sdbFileCl;
    private final ExecutorService threadPool;
    private final Semaphore semp;
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
    private ConcurrentHashMap<String, FileInfoWrapper> submitFiles;

    private int fileStatusCheckBatchSize = 100;

    private ConcurrentLinkedQueue<ScmSessionWrapper> sessions;


    public FileReadFromSrcSite(int batchSize, long fileTimeout, int thread, int queueSize,
            String localSiteName, final String wsName, BSONObject matcher,
            final List<String> urlList, final String user, final String password,
            int fileStatusCheckInterval, List<String> sdbCoord, String sdbUser, String sdbPassword,
            int fileStatusCheckBatchSize)
            throws ScmException, ScmMappingException {
        this.wsName = wsName;
        this.fileTimeout = fileTimeout;
        this.scope = ScmType.ScopeType.SCOPE_CURRENT;
        this.urlList = urlList;
        this.user = user;
        this.password = password;
        this.batchSize = batchSize;
        this.semp = new Semaphore(batchSize);
        submitFiles = new ConcurrentHashMap(batchSize);
        this.fileStatusCheckBatchSize = fileStatusCheckBatchSize;

        sdb = new Sequoiadb(sdbCoord, sdbUser, sdbPassword, new ConfigOptions());
        sdbFileCl = sdb.getCollectionSpace(wsName + "_META").getCollection("FILE");
        this.siteInfoMgr = new SiteInfoMgr(sdb);
        localSiteId = siteInfoMgr.getSiteIdByName(localSiteName);
        BSONObject[] not = { new BasicBSONObject("site_list.$0.site_id", localSiteId) };
        BSONObject[] and = { new BasicBSONObject("$not", not) };
        matcher.put("$and", and);
        this.matcher = matcher;
        this.siteInfoMgr = new SiteInfoMgr(sdb);

        sessions = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < thread; i++) {
            sessions.add(createScmSessionWrapper());
        }

        ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(queueSize);
        threadPool = new ThreadPoolExecutor(thread, thread, 60000,
                TimeUnit.MICROSECONDS, taskQueue, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    taskQueue.put(r);
                } catch (InterruptedException e) {
                    logger.warn("failed to add task queue, run by main thread");
                    r.run();
                }
            }
        });
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
        if (threadPool != null) {
            threadPool.shutdown();
        }
        if (sessions != null) {
            for (ScmSessionWrapper s : sessions) {
                s.getSession().close();
            }
        }
    }

    private ScmSessionWrapper createScmSessionWrapper() throws ScmException {
        ScmSession session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION, new ScmConfigOption(urlList, user, password));
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(wsName, session);
            return new ScmSessionWrapper(session, workspace);
        } catch (Exception e) {
            session.close();
            throw e;
        }
    }

    private void readFile(ScmFileBasicInfo file) {
        try {
            boolean isTempSession = false;
            ScmSessionWrapper sessionWrapper = sessions.poll();
            if (sessionWrapper == null) {
                logger.info("use temp session");
                sessionWrapper = createScmSessionWrapper();
                isTempSession = true;
            }
            try {
                ScmFile fileObj = ScmFactory.File.getInstance(sessionWrapper.getWs(), file.getFileId());
                if (isContainLocalSite(fileObj)) {
                    return;
                }
                fileObj.getContent(new NoneOs());
            } catch (Exception e) {
                if (!isTempSession) {
                    sessionWrapper.getSession().close();
                    sessions.add(createScmSessionWrapper());
                }
                throw e;
            } finally {
                if (isTempSession) {
                    sessionWrapper.getSession().close();
                } else {
                    sessions.add(sessionWrapper);
                }
            }
        } catch (Throwable e) {
            logger.error("failed to transfer file: fileId={}, version={}.{}",
                    file.getFileId(), file.getMajorVersion(), file.getMinorVersion(), e);
        }
    }

    public void transfer() throws ScmException, InterruptedException {
        ScmCursor<ScmFileBasicInfo> cursor = null;
        ScmSession session = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION, new ScmConfigOption(urlList, user, password));
        try {
            ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace(wsName, session);
            cursor = ScmFactory.File.listInstance(workspace, scope, matcher);
            while (cursor.hasNext()) {
                ScmFileBasicInfo file = cursor.getNext();
                semp.acquire();
                threadPool.submit(() -> {
                    readFile(file);
                });
                submitFiles.put(file.getFileId().get(), new FileInfoWrapper(file));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            session.close();
        }
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
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
            try {
                cursor.close();
            } catch (Exception e) {
            }
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

    private boolean isContainLocalSite(ScmFile file) {
        for (ScmFileLocation location : file.getLocationList()) {
            if (location.getSiteId() == localSiteId) {
                return true;
            }
        }
        return false;
    }
}


class NoneOs extends OutputStream {

    @Override
    public void write(int b) throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
