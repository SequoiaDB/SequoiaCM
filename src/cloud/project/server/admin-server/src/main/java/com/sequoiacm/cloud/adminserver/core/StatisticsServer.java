package com.sequoiacm.cloud.adminserver.core;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.cloud.adminserver.lock.LockPathFactory;
import com.sequoiacm.cloud.adminserver.model.ObjectDeltaInfo;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.config.client.cache.bucket.BucketConfCache;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilter;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.dao.ContentServerDao;
import com.sequoiacm.cloud.adminserver.dao.StatisticsDao;
import com.sequoiacm.cloud.adminserver.dao.WorkspaceDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;
import com.sequoiacm.cloud.adminserver.model.TrafficInfo;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;

public class StatisticsServer {
    private static Logger logger = LoggerFactory.getLogger(StatisticsServer.class);
    private static StatisticsServer INSTANCE = new StatisticsServer();

    private ContentServerDao contentServerDao;
    private StatisticsDao statisticsDao;
    private WorkspaceDao workspaceDao;
    private ScmLockManager lockManager;
    private LockPathFactory lockPathFactory;

    private BucketConfCache bucketConfCache;

    private StatisticsServer() {
    }

    public static StatisticsServer getInstance() {
        return INSTANCE;
    }

    public void init(ContentServerDao contentServerDao, StatisticsDao statisticsDao,
            WorkspaceDao workspaceDao, BucketConfCache bucketConfCache,
            ScmLockManager lockManager, LockPathFactory lockPathFactory)
            throws StatisticsException {
        this.contentServerDao = contentServerDao;
        this.statisticsDao = statisticsDao;
        this.workspaceDao = workspaceDao;
        this.bucketConfCache = bucketConfCache;
        this.lockManager = lockManager;
        this.lockPathFactory = lockPathFactory;
    }
    
    public List<ContentServerInfo> getContentServers() throws StatisticsException {
        return contentServerDao.queryAll();
    }

    /*public List<ContentServerInfo> getContentServer(int siteId) {
        List<ContentServerInfo> serverList = new ArrayList<>();
        for (ContentServerInfo csInfo : contentServerList) {
            if (csInfo.getSiteId() == siteId) {
                serverList.add(csInfo);
            }
        }

        return serverList;
    }*/
    
    public List<WorkspaceInfo> getWorkspaces() throws StatisticsException {
        return workspaceDao.query();
    }

    public List<String> getBuckets() throws StatisticsException {
        ScmObjectCursor<BucketConfig> cursor = null;
        try {
            List<String> bucketConfigs = new ArrayList<>();
            cursor = bucketConfCache.listBucket(null, null, 0, -1);
            while (cursor.hasNext()) {
                bucketConfigs.add(cursor.getNext().getName());
            }
            return bucketConfigs;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR, "failed to get buckets",
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public BucketConfig getBucket(String bucketName) throws StatisticsException {
        try {
            return bucketConfCache.getBucket(bucketName);
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to get bucket,bucket=" + bucketName, e);
        }
    }
    
    public WorkspaceInfo getWorkspaceChecked(String wsName) throws StatisticsException {
        WorkspaceInfo wsInfo = getWorkspace(wsName);
        if (null == wsInfo) {
            throw new StatisticsException(StatisticsError.WORKSPACE_NOT_EXISTS,
                    "workspace is not exist:name=" + wsName);
        }

        return wsInfo;
    }
    
    public WorkspaceInfo getWorkspace(String wsName) throws StatisticsException {
        return workspaceDao.queryByName(wsName);
    }
    
    public TrafficInfo getLastTrafficRecord(String type, String workspace)
            throws StatisticsException {
        return statisticsDao.queryLastTrafficRecord(type, workspace);
    }
    
    public FileDeltaInfo getLastFileDeltaRecord(String workspace)
            throws StatisticsException {
        return statisticsDao.queryLastFileDeltaRecord(workspace);
    }

    public ObjectDeltaInfo getLastObjectDeltaRecord(String bucketName) throws StatisticsException {
        return statisticsDao.queryLastObjectDeltaRecord(bucketName);
    }
    
    public void upsertTrafficRecord(String type, String workspace, long recordTime, long newTraffic)
            throws StatisticsException {
        this.statisticsDao.upsertTraffic(type, workspace, recordTime, newTraffic);
    }

    public void upsertFileDeltaRecord(String workspace, long recordTime, long newCount, long newSize)
            throws StatisticsException {
        this.statisticsDao.upsertFileDelta(workspace, recordTime, newCount, newSize);
    }

    public void upsertObjectDeltaRecord(String bucketName, long recordTime, long newCount,
            long newSize) throws StatisticsException {
        this.statisticsDao.upsertObjectDelta(bucketName, recordTime, newCount, newSize);
    }

    public ScmLockManager getLockManager() {
        return lockManager;
    }

    public LockPathFactory getLockPathFactory() {
        return lockPathFactory;
    }
    
    /*
    private void initSiteInfo() throws Exception {
        MetaCursor siteCursor = null;
        try {
            siteCursor = siteDao.query(null);
            while (siteCursor.hasNext()) {
                BSONObject obj = siteCursor.getNext();
                SiteInfo site = BsonTranslator.Site.fromBSONObject(obj);
                List<ContentServerInfo> serverList = getContentServerBySite(site.getId());
                site.setServers(serverList);
                siteMgr.addSite(site);
            }
        }
        finally {
            if (null != siteCursor) {
                siteCursor.close();
            }
        }
    }

    private List<ContentServerInfo> getContentServerBySite(int siteId) throws StatisticsException {
        List<ContentServerInfo> serverList = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject(FieldName.ContentServer.FIELD_SITE_ID,
                siteId);
        MetaCursor serverCursor = contentServerDao.query(matcher);
        try {
            while (serverCursor.hasNext()) {
                BSONObject obj = serverCursor.getNext();
                ContentServerInfo csInfo = BsonTranslator.ContentServer
                        .fromBSONObject(obj);
                serverList.add(csInfo);
            }
            return serverList;
        }
        finally {
            if (null != serverCursor) {
                serverCursor.close();
            }
        }
    }

    public ContentServerInfo getRandomServer(int siteId) {
        SiteInfo siteInfo = siteMgr.getSite(siteId);
        if (null == siteInfo) {
            return null;
        }

        List<ContentServerInfo> serverList = siteInfo.getServers();
        if (null == serverList) {
            return null;
        }

        Random r = new Random();
        int idx = r.nextInt(serverList.size());
        return serverList.get(idx);
    }

    public SiteInfo getRootSite() {
        return siteMgr.getMainSite();
    }
    */
}
