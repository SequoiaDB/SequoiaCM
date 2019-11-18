package com.sequoiacm.schedule.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.core.meta.NodeMgr;
import com.sequoiacm.schedule.core.meta.SiteInfo;
import com.sequoiacm.schedule.core.meta.SiteMgr;
import com.sequoiacm.schedule.core.meta.WorkspaceInfo;
import com.sequoiacm.schedule.core.meta.WorkspaceMgr;
import com.sequoiacm.schedule.dao.FileServerDao;
import com.sequoiacm.schedule.dao.SiteDao;
import com.sequoiacm.schedule.dao.StrategyDao;
import com.sequoiacm.schedule.dao.TaskDao;
import com.sequoiacm.schedule.dao.WorkspaceDao;
import com.sequoiacm.schedule.entity.ConfigEntityTranslator;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.entity.SiteEntity;
import com.sequoiacm.schedule.entity.TaskEntity;
import com.sequoiacm.schedule.entity.WorkspaceEntity;

public class ScheduleServer {
    private static Logger logger = LoggerFactory.getLogger(ScheduleServer.class);
    private static ScheduleServer instance = new ScheduleServer();

    private FileServerDao fileServerDao;
    private SiteDao siteDao;
    private WorkspaceDao workspaceDao;
    private TaskDao taskDao;
    private StrategyDao strategyDao;

    private WorkspaceMgr workspaceMgr = new WorkspaceMgr();
    private SiteMgr siteMgr = new SiteMgr();
    private NodeMgr nodeMgr = new NodeMgr();

    private ScheduleServer() {
    }

    public static ScheduleServer getInstance() {
        return instance;
    }

    public void init(SiteDao siteDao, WorkspaceDao workspaceDao, FileServerDao fileServerDao,
            TaskDao taskDao, StrategyDao strategyDao) throws Exception {
        this.workspaceDao = workspaceDao;
        this.fileServerDao = fileServerDao;
        this.siteDao = siteDao;
        this.taskDao = taskDao;
        this.strategyDao = strategyDao;
        initSiteMgr();
        initWorkspaceMgr();
        initServerNodeMgr();
    }

    private Map<Integer, SiteEntity> getSiteList() throws Exception {
        Map<Integer, SiteEntity> siteMap = new HashMap<>();

        ScmBSONObjectCursor siteCursor = null;
        try {
            siteCursor = siteDao.query(new BasicBSONObject());
            while (siteCursor.hasNext()) {
                BSONObject obj = siteCursor.next();
                SiteEntity site = ConfigEntityTranslator.Site.fromBSONObject(obj);
                siteMap.put(site.getId(), site);
            }

            return siteMap;
        }
        finally {
            if (null != siteCursor) {
                siteCursor.close();
            }
        }
    }

    private void initSiteMgr() throws Exception {
        Map<Integer, SiteEntity> siteMapEntity = getSiteList();
        Map<Integer, SiteInfo> siteMap = new HashMap<>();
        Map<String, SiteInfo> siteMapByName = new HashMap<>();
        for (SiteEntity siteEntity : siteMapEntity.values()) {
            SiteInfo info = transformSiteInfo(siteEntity);
            if (info.isRoot()) {
                siteMgr.initRootSite(info);
            }
            siteMap.put(info.getId(), info);
            siteMapByName.put(info.getName(), info);
        }
        siteMgr.initSiteMap(siteMap, siteMapByName);
    }

    private void initWorkspaceMgr() throws Exception {
        HashMap<String, WorkspaceInfo> initWsMap = new HashMap<>();
        ScmBSONObjectCursor workspaceCursor = null;
        try {
            workspaceCursor = workspaceDao.query(new BasicBSONObject());
            while (workspaceCursor.hasNext()) {
                BSONObject obj = workspaceCursor.next();
                WorkspaceInfo wsInfo = createWorkspaceInfo(obj);
                initWsMap.put(wsInfo.getName(), wsInfo);
            }
        }
        finally {
            if (null != workspaceCursor) {
                workspaceCursor.close();
            }
        }

        workspaceMgr.init(initWsMap);
    }

    private void initServerNodeMgr() throws Exception {
        Map<String, FileServerEntity> ServerMapByName = new HashMap<>();
        Map<Integer, Map<String, FileServerEntity>> ServerMapBySiteId = new HashMap<>();
        ScmBSONObjectCursor serverNodeCursor = null;
        try {
            serverNodeCursor = fileServerDao.query(new BasicBSONObject());
            while (serverNodeCursor.hasNext()) {
                BSONObject obj = serverNodeCursor.next();
                FileServerEntity serverNode = ConfigEntityTranslator.FileServer.fromBSONObject(obj);
                ServerMapByName.put(serverNode.getName(), serverNode);
                // ServerMapBySiteId <Integer,Map<String,FileServerEntity>>
                Map<String, FileServerEntity> tmpSite2Node = ServerMapBySiteId
                        .get(serverNode.getSiteId());
                if (tmpSite2Node == null) {
                    tmpSite2Node = new HashMap<>();
                    tmpSite2Node.put(serverNode.getName(), serverNode);
                    ServerMapBySiteId.put(serverNode.getSiteId(), tmpSite2Node);
                }
                else {
                    tmpSite2Node.put(serverNode.getName(), serverNode);
                }
            }
        }
        finally {
            if (null != serverNodeCursor) {
                serverNodeCursor.close();
            }
        }
        nodeMgr.initNodeSreverMap(ServerMapBySiteId, ServerMapByName);
    }

    public WorkspaceInfo getWorkspace(String wsName) {
        return workspaceMgr.getWsInfo(wsName);
    }

    public FileServerEntity getRandomServer(int siteId) {
        SiteInfo siteInfo = siteMgr.getSite(siteId);
        if (null == siteInfo) {
            return null;
        }

        List<FileServerEntity> serverList = siteInfo.getServers();
        if (null == serverList || serverList.size() == 0) {
            return null;
        }

        Random r = new Random();
        int idx = r.nextInt(serverList.size());
        return serverList.get(idx);
    }

    public void insertTask(TaskEntity info) throws Exception {
        taskDao.insert(info);
    }

    public void deleteTask(String taskId) throws Exception {
        taskDao.delete(taskId);
    }

    public boolean isTaskExist(BSONObject condition) throws Exception {
        ScmBSONObjectCursor cursor = null;
        try {
            cursor = taskDao.query(condition);
            if (cursor.hasNext()) {
                return true;
            }
            else {
                return false;
            }
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public List<BSONObject> getAllStrategy() throws Exception {
        ScmBSONObjectCursor cursor = null;
        List<BSONObject> strategyList = new ArrayList<>();
        try {
            cursor = strategyDao.query(null);
            if (cursor.hasNext()) {
                strategyList.add(cursor.next());
            }
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return strategyList;
    }

    public SiteInfo getRootSite() {
        return siteMgr.getMainSite();
    }

    public void reloadWorkspace(String wsName) throws Exception {
        logger.info("refresh workspace cache:wsName={}", wsName);
        ScmBSONObjectCursor wsCursor = workspaceDao
                .query(new BasicBSONObject(FieldName.Workspace.FIELD_NAME, wsName));

        try {
            if (wsCursor.hasNext()) {
                BSONObject wsRec = wsCursor.next();
                WorkspaceInfo wsInfo = createWorkspaceInfo(wsRec);
                workspaceMgr.reloadWorkspace(wsInfo);

            }
            else {
                workspaceMgr.removeWorkspace(wsName);
            }
        }
        finally {
            wsCursor.close();
        }
    }

    public void removeWorkspace(String wsName) {
        workspaceMgr.removeWorkspace(wsName);
    }

    private WorkspaceInfo createWorkspaceInfo(BSONObject wsRec) throws Exception {
        WorkspaceEntity workspaceEntity = ConfigEntityTranslator.Workspace.fromBSONObject(wsRec);
        WorkspaceInfo wsInfo = new WorkspaceInfo(workspaceEntity.getId(),
                workspaceEntity.getName());

        for (int siteId : workspaceEntity.getSiteList()) {
            SiteInfo siteInfo = siteMgr.getSite(siteId);
            if (null == siteInfo) {
                throw new Exception("can't find site in workspace:workspace="
                        + workspaceEntity.getName() + ",site_id=" + siteId);
            }
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setId(siteInfo.getId());
            siteEntity.setName(siteInfo.getName());
            siteEntity.setRoot(siteInfo.isRoot());
            wsInfo.addSite(siteEntity);
        }
        return wsInfo;
    }

    public void reloadSite(String siteName) throws Exception {
        SiteEntity siteEntity = siteDao.queryOne(siteName);
        if (siteEntity != null) {
            SiteInfo siteInfo = transformSiteInfo(siteEntity);
            siteMgr.reloadSite(siteInfo);
        }
        else {
            siteMgr.removeSite(siteName);
        }
    }

    public void removeSite(String siteName) throws Exception {
        siteMgr.removeSite(siteName);
    }

    private SiteInfo transformSiteInfo(SiteEntity siteEntity) throws Exception {
        SiteInfo siteInfo = new SiteInfo(siteEntity.getId(), siteEntity.getName(),
                siteEntity.isRoot());
        return siteInfo;
    }

    public void reloadNode(String nodeName) throws Exception {
        FileServerEntity serverNode = fileServerDao.queryOne(nodeName);
        if (serverNode != null) {
            nodeMgr.reloadNode(serverNode);
        }
        else {
            nodeMgr.removeNode(nodeName);
        }
    }

    public void removeNode(String nodeName) throws Exception {
        nodeMgr.removeNode(nodeName);
    }

    public List<FileServerEntity> getServersBySiteId(int siteId) {
        return nodeMgr.getServersBySiteId(siteId);
    }

    public void removeNodesBySiteId(int siteId) {
        nodeMgr.removeNodesBySiteId(siteId);
    }
}
