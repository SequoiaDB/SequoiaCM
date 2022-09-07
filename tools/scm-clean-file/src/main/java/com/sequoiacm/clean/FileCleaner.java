package com.sequoiacm.clean;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.lock.ScmLockConfig;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;
import com.sequoiacm.infrastructure.lock.curator.CuratorZKCleaner;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.sequoiadb.dataopertion.SdbDataOpFactoryImpl;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FileCleaner {
    private static final Logger logger = LoggerFactory.getLogger(FileCleaner.class);

    private static final String passwordFile = "./scm-clean-pwd.file";
    private final SequoiadbDatasource metaSdbDs;
    private final SiteInfoMgr siteMgr;
    private final SdbDataService cleanSiteDataService;
    private final String wsName;
    private final BSONObject fileMatcher;
    private ThreadPoolExecutor threadPool;
    private final ScmLockManager lockMgr;
    private final int cleanSiteId;
    private final String cleanSiteName;
    private final SdbDataOpFactoryImpl sdbDataOpFactory;
    private ScmFeignUtil feignUtil;
    private FileCounter fileCounter = new FileCounter();
    // private List<String> metaSdbCoord = null;
    // private String metaSdbUser;
    // private String metaSdbPassword;
    //
    // private List<String> cleanSiteLobSdbCoord;
    // private String cleanSiteLobSdbUser;
    // private String cleanSiteLobSdbPasswrd;

    public FileCleaner(String wsName, BSONObject fileMatcher, int cleanSiteId, int queueSize,
            int thread, String metaSdbPassword, String metaSdbUser, List<String> metaSdbCoord,
            String cleanSiteLobSdbUser, String cleanSiteLobSdbPasswd,
            List<String> cleanSiteLobSdbCoordList, String zkUrls, int dataInOtherSiteId,
            List<String> dataInOtherSiteInstanceList) throws Exception {
        this.wsName = wsName;
        this.fileMatcher = fileMatcher;
        this.cleanSiteId = cleanSiteId;
        try {
            metaSdbDs = new SequoiadbDatasource(metaSdbCoord, metaSdbUser, metaSdbPassword,
                    new ConfigOptions(), new DatasourceOptions());
            // SdbDataOpFactoryImpl factory = new SdbDataOpFactoryImpl();
            this.siteMgr = new SiteInfoMgr(metaSdbDs, wsName, cleanSiteId, dataInOtherSiteId,
                    dataInOtherSiteInstanceList);
            this.cleanSiteName = siteMgr.getSitNameById(cleanSiteId);
            SdbSiteUrl targetSiteUrl = new SdbSiteUrl(
                    CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR,
                    cleanSiteLobSdbCoordList, cleanSiteLobSdbUser,
                    genPasswordFile(cleanSiteLobSdbUser, cleanSiteLobSdbPasswd),
                    new com.sequoiadb.net.ConfigOptions(), new DatasourceOptions());
            this.cleanSiteDataService = new SdbDataService(cleanSiteId, targetSiteUrl);
            this.sdbDataOpFactory = new SdbDataOpFactoryImpl();
            ScmLockConfig lockConf = new ScmLockConfig();
            lockConf.setUrls(zkUrls);
            lockConf.setDisableJob(true);
            this.lockMgr = new ScmLockManager(lockConf);
            if (!CuratorZKCleaner.isInitialized()) {
                CuratorZKCleaner.init(new CuratorLockFactory(zkUrls).getCuratorClient(),
                        lockConf.getCoreCleanThreads(),
                        lockConf.getMaxCleanThreads(), lockConf.getCleanQueueSize());
            }
            this.feignUtil = new ScmFeignUtil();
            final ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(queueSize);
            threadPool = new ThreadPoolExecutor(thread, thread, 60000, TimeUnit.MICROSECONDS,
                    taskQueue, new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            try {
                                taskQueue.put(r);
                            }
                            catch (InterruptedException e) {
                                logger.warn("failed to add task queue, run by main thread");
                                r.run();
                            }
                        }
                    });
        }
        catch (Exception e) {
            destroy();
            throw e;
        }
    }

    public void destroy() {
        try {
            if (threadPool != null) {
                threadPool.shutdown();
            }
            if (metaSdbDs != null) {
                metaSdbDs.close();
            }
            if (cleanSiteDataService != null) {
                cleanSiteDataService.clear();
            }
            if (lockMgr != null) {
                lockMgr.close();
            }
        }
        catch (Exception e) {
            logger.error("failed to destroy instance", e);
        }

    }

    public void clean() throws InterruptedException, ScmLockException {
        RemoteClientMgr remoteClientMgr = new RemoteClientMgr();
        Sequoiadb sdb = metaSdbDs.getConnection();
        DBCursor cursor = null;
        try {
            DBCollection fileCl = sdb.getCollectionSpace(wsName + "_META").getCollection("FILE");
            cursor = fileCl.query(fileMatcher, null, null, null);
            while (cursor.hasNext()) {
                BSONObject fileInfoNotInLock = cursor.getNext();
                TaskCleanFile312 task = new TaskCleanFile312(metaSdbDs, wsName, lockMgr,
                        cleanSiteName, cleanSiteId, sdbDataOpFactory,
                        siteMgr.getCleanSiteDataLocation(), cleanSiteDataService, feignUtil,
                        siteMgr, fileCounter,
                        BsonUtils.getStringChecked(fileInfoNotInLock, FieldName.FIELD_CLFILE_ID),
                        BsonUtils.getNumberChecked(fileInfoNotInLock,
                                FieldName.FIELD_CLFILE_MAJOR_VERSION).intValue(),
                        BsonUtils.getNumberChecked(fileInfoNotInLock,
                                FieldName.FIELD_CLFILE_MINOR_VERSION).intValue(),
                        BsonUtils.getStringChecked(fileInfoNotInLock,
                                FieldName.FIELD_CLFILE_FILE_DATA_ID),
                        remoteClientMgr);
                threadPool.submit(task);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            metaSdbDs.releaseConnection(sdb);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
        threadPool = null;
        fileCounter.logCounterAtFinish();
    }

    private static String genPasswordFile(String user, String password) throws Exception {
        String encryptPwd = user + ":"
                + ScmPasswordMgr.getInstance().encrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password);
        File file = new File(passwordFile);
        FileOutputStream os = new FileOutputStream(file);
        os.write(encryptPwd.getBytes());
        os.flush();
        os.close();
        return file.getAbsolutePath();
    }
}
