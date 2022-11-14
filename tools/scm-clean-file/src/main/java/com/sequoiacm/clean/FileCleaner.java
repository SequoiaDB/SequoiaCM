package com.sequoiacm.clean;

import com.sequoiacm.common.mapping.ScmSiteObj;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.infrastructure.lock.ScmLockConfig;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiadb.net.ConfigOptions;
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
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FileCleaner {
    private static final Logger logger = LoggerFactory.getLogger(FileCleaner.class);

    private static final String passwordFile = "./scm-clean-pwd.file";
    private final SequoiadbDatasource metaSdbDs;
    private final SiteInfoMgr siteMgr;
    private final ScmService cleanSiteDataService;
    private final String wsName;
    private final BSONObject fileMatcher;
    private ThreadPoolExecutor threadPool;
    private final ScmLockManager lockMgr;
    private final int cleanSiteId;
    private final int holdingDataSiteId;
    private final ScmDataOpFactory scmDataOpFactory;
    private final FileCounter fileCounter = new FileCounter();
    private final ScmShutDownHook shutDownHook;

    public FileCleaner(String wsName, BSONObject fileMatcher, List<String> dataInOtherSiteInstanceList,
            String cleanSiteName, String holdingDataSiteName, int queueSize, int thread, String srcSitePasswordFile,
            String srcSitePassword, String metaSdbPassword, String metaSdbUser,
            List<String> metaSdbCoord, int connectTimeout, int socketTimeout, int maxConnectionNum,
            int keepAliveTimeout, String zkUrls, Map<String, String> datasourceConf) throws Exception {
        this.wsName = wsName;
        try {
            ConfigOptions sdbConnConf = new ConfigOptions();
            sdbConnConf.setConnectTimeout(connectTimeout);
            sdbConnConf.setSocketTimeout(socketTimeout);

            DatasourceOptions sdbDatasourceConf = new DatasourceOptions();
            sdbDatasourceConf.setMaxCount(maxConnectionNum);
            sdbDatasourceConf.setKeepAliveTimeout(keepAliveTimeout);
            List<String> preferedInstance = new ArrayList<>();
            preferedInstance.add("M");
            sdbDatasourceConf.setPreferedInstance(preferedInstance);

            metaSdbDs = new SequoiadbDatasource(metaSdbCoord, metaSdbUser, metaSdbPassword,
                    sdbConnConf, sdbDatasourceConf);
            this.siteMgr = new SiteInfoMgr(metaSdbDs, wsName, cleanSiteName, dataInOtherSiteInstanceList);
            ScmSiteObj siteObj = siteMgr.getSiteObjByName(cleanSiteName);
            this.cleanSiteId = siteObj.getId();
            this.holdingDataSiteId = siteMgr.getSiteObjByName(holdingDataSiteName).getId();
            BasicBSONList andList = new BasicBSONList();
            andList.add(fileMatcher);
            andList.add(new BasicBSONObject("site_list.$1.site_id", cleanSiteId));
            this.fileMatcher = new BasicBSONObject("$and", andList);
            if (!StringUtils.isEmpty(srcSitePassword)) {
                srcSitePasswordFile = genPasswordFile(siteObj.getDataUser(), srcSitePassword);
            }
            ScmSiteUrl cleanSiteUrl = ScmDatasourceUtil.createSiteUrl(siteObj,
                    srcSitePasswordFile, sdbConnConf, sdbDatasourceConf, datasourceConf);
            this.scmDataOpFactory = ScmDatasourceUtil.createDataOpFactory(siteObj.getDataType());
            this.cleanSiteDataService = ScmDatasourceUtil.createDataService(cleanSiteId, cleanSiteUrl);
            ScmLockConfig lockConf = new ScmLockConfig();
            lockConf.setUrls(zkUrls);
            this.lockMgr = new ScmLockManager(lockConf);
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
            this.shutDownHook = new ScmShutDownHook(threadPool);
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
                TaskCleanFile task = new TaskCleanFile(metaSdbDs, wsName, lockMgr,
                        cleanSiteId, holdingDataSiteId, scmDataOpFactory, siteMgr.getCleanSiteDataLocation(),
                        cleanSiteDataService, siteMgr, fileCounter, fileInfoNotInLock,
                        remoteClientMgr, threadPool, shutDownHook);
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
