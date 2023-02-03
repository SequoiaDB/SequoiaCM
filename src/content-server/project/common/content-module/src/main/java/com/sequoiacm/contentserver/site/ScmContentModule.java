package com.sequoiacm.contentserver.site;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.model.ScmWorkspaceItem;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.*;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.IMetaSourceHandler;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ScmContentModule {
    private static final Logger logger = LoggerFactory.getLogger(ScmContentModule.class);

    private ScmBizConf bizConf = new ScmBizConf();
    private ScmSiteMgr siteMgr = new ScmSiteMgr();

    private ReentrantReadWriteLock wsReadWriteLock = new ReentrantReadWriteLock();
    private Map<Integer, ScmWorkspaceInfo> workspaceMap = new HashMap<>();
    private Map<String, ScmWorkspaceInfo> workspaceMapByName = new HashMap<>();
    private static IMetaSourceHandler metaSourceHandler;

    private static ScmContentModule contentModule = null;
    private static String mySiteName;

    private ScmContentModule() {
    }

    public static void bindSite(String site) {
        mySiteName = site;
    }

    public static void metaSourceHandler(IMetaSourceHandler handler) {
        metaSourceHandler = handler;
    }

    public static void reload() throws ScmServerException {
        synchronized (ScmContentModule.class) {
            ScmContentModule newContentModule = new ScmContentModule();
            try {
                newContentModule.initBizConf();
                if (isSiteEquals(contentModule.bizConf, newContentModule.bizConf)) {
                    newContentModule.clear();
                    newContentModule = null;
                    logger.info("business configuration haven't changed, reloadbizconf do noting");
                }
                else {
                    newContentModule.initSiteMgr();
                    newContentModule.initWorkspaceInfo();
                    newContentModule.activeMetaSourceHandler();
                    ScmContentModule tmp = contentModule;
                    contentModule = newContentModule;
                    tmp.clear();
                    logger.info("server init success:rootSiteId={},mySite={}",
                            contentModule.bizConf.getRootSiteId(),
                            contentModule.bizConf.getMySite().getId());
                }
            }
            catch (ScmServerException e) {
                e.resetError(ScmError.SERVER_RELOAD_CONF_FAILED);
                throw e;
            }
            catch (Exception e) {
                throw new ScmServerException(ScmError.SERVER_RELOAD_CONF_FAILED,
                        "reload ScmContentServer failed", e);
            }
        }
    }

    private void activeMetaSourceHandler() throws ScmServerException {
        if (metaSourceHandler != null) {
            getMetaService().getMetaSource().activeHandler(metaSourceHandler);
        }
    }

    private static boolean isSiteEquals(ScmBizConf oldConf, ScmBizConf newConf) {
        Map<Integer, ScmSite> oldSiteMap = oldConf.getSiteMap();
        Map<Integer, ScmSite> newSiteMap = newConf.getSiteMap();
        if (oldSiteMap.size() != newSiteMap.size()) {
            return false;
        }

        for (ScmSite oldOne : oldSiteMap.values()) {
            ScmSite newOne = newSiteMap.get(oldOne.getId());
            if (!oldOne.equals(newOne)) {
                return false;
            }
        }

        List<BSONObject> oldWorkspaceList = oldConf.getWorkspaceList();
        List<BSONObject> newWorkspaceList = newConf.getWorkspaceList();
        if (oldWorkspaceList.size() != newWorkspaceList.size()) {
            return false;
        }

        // WorkspaceList is ordered in ScmBizConf.inif()
        for (int i = 0; i < oldWorkspaceList.size(); i++) {
            BSONObject oldObj = oldWorkspaceList.get(i);
            BSONObject newObj = newWorkspaceList.get(i);
            if (!oldObj.equals(newObj)) {
                return false;
            }
        }

        return true;
    }

    public static ScmContentModule getInstance() {
        if (mySiteName == null) {
            throw new RuntimeException("bind site first!");
        }
        if (null == contentModule) {
            synchronized (ScmContentModule.class) {
                if (null == contentModule) {
                    ScmContentModule tmp = new ScmContentModule();
                    try {
                        tmp.init();
                    }
                    catch (Exception e) {
                        logger.error("initial ContentServer failed", e);
                        contentModule = null;
                        System.exit(-1);
                    }
                    contentModule = tmp;
                }
            }
        }

        return contentModule;
    }

    private void clear() {
        bizConf.clear();
        siteMgr.clear();
    }

    private void initBizConf() throws ScmServerException {
        bizConf.init(mySiteName);
    }

    private void loadWorkspaceInfo(List<BSONObject> workspaceList) throws ScmServerException {
        logger.info("start to parse workspace info");
        for (int i = 0; i < workspaceList.size(); i++) {
            ScmWorkspaceInfo info = new ScmWorkspaceInfo(bizConf, workspaceList.get(i));
            WriteLock wLock = wsReadWriteLock.writeLock();
            wLock.lock();
            try {
                workspaceMap.put(info.getId(), info);
                workspaceMapByName.put(info.getName(), info);
            }
            finally {
                wLock.unlock();
            }
        }
    }

    private void validateWorkspaceInfo() throws ScmServerException {
        MetaSourceLocation firstLocation = null;
        for (ScmWorkspaceInfo wsInfo : getWorkspaceInfos()) {
            MetaSourceLocation location = wsInfo.getMetaLocation();
            if (null == location) {
                throw new ScmInvalidArgumentException(
                        "meta location is not exist:ws=" + wsInfo.getName());
            }

            if (!location.getType().equals(ScmDataSourceType.SEQUOIADB.getName())) {
                throw new ScmInvalidArgumentException(
                        "meta location must be sequoiadb:location=" + location);
            }

            if (null == firstLocation) {
                firstLocation = location;
            }
            else {
                if (firstLocation.getSiteId() != location.getSiteId()) {
                    throw new ScmInvalidArgumentException(
                            "all workspace's meta location must be the same site now:preLocation=["
                                    + firstLocation + "],location=[" + location + "]");
                }
            }
        }
    }

    private List<ScmWorkspaceInfo> getWorkspaceInfos() {
        ReadLock rLock = wsReadWriteLock.readLock();
        rLock.lock();
        try {
            return new ArrayList<>(workspaceMap.values());
        }
        finally {
            rLock.unlock();
        }
    }

    private void initWorkspaceInfo() throws ScmServerException {
        loadWorkspaceInfo(bizConf.getWorkspaceList());
        validateWorkspaceInfo();
    }

    private void init() throws Exception {
        try {
            initBizConf();
            initSiteMgr();
            initWorkspaceInfo();
            checkAndAmendDirVersion();
            activeMetaSourceHandler();
            logger.info("server init success:rootSiteId={},mySiteId={}", bizConf.getRootSiteId(),
                    bizConf.getMySite().getName());
        }
        catch (ScmServerException e) {
            clear();
            throw e;
        }
        catch (Exception e) {
            clear();
            throw new ScmSystemException("init contentModule failed", e);
        }
    }

    private void checkAndAmendDirVersion() throws ScmServerException {
        for (ScmWorkspaceInfo ws : getWorkspaceInfos()) {
            if (!ws.isEnableDirectory()) {
                continue;
            }
            try {
                getMetaService().getMetaSource().getDirAccessor(ws.getName())
                        .checkAndAmendVersion();
            }
            catch (ScmMetasourceException e) {
                throw new ScmServerException(e.getScmError(),
                        "check and amend directory version failed, wsName=" + ws.getName(), e);
            }
        }
    }

    public List<String> getWorkspaceNames() {
        ReadLock rLock = wsReadWriteLock.readLock();
        rLock.lock();
        try {
            return new ArrayList<>(workspaceMapByName.keySet());
        }
        finally {
            rLock.unlock();
        }
    }

    public void removeWorkspace(String wsName) {
        WriteLock wLock = wsReadWriteLock.writeLock();
        wLock.lock();
        try {
            ScmWorkspaceInfo oldWs = workspaceMapByName.remove(wsName);
            if (oldWs != null) {
                workspaceMap.remove(oldWs.getId());
            }
            bizConf.removeWorkspace(wsName);
        }
        finally {
            wLock.unlock();
        }
    }

    public void reloadWorkspace(String wsName) throws ScmServerException {
        WriteLock wLock = wsReadWriteLock.writeLock();
        wLock.lock();
        try {
            logger.info("refresh workspace cache:wsName={}", wsName);

            BSONObject wsObj = bizConf.reloadWorkspace(wsName);
            if (wsObj == null) {
                ScmWorkspaceInfo ws = workspaceMapByName.remove(wsName);
                if (ws != null) {
                    workspaceMap.remove(ws.getId());
                }
            }
            else {
                ScmWorkspaceInfo newWsInfo = new ScmWorkspaceInfo(bizConf, wsObj);
                workspaceMap.put(newWsInfo.getId(), newWsInfo);
                workspaceMapByName.put(wsName, newWsInfo);
            }

        }
        finally {
            wLock.unlock();
        }
    }

    public void workspaceMapAddHistory(ScmWorkspaceItem hisWsItem) {
        ReentrantReadWriteLock.WriteLock wLock = wsReadWriteLock.writeLock();
        wLock.lock();
        try {
            ScmWorkspaceInfo workspaceInfoById = workspaceMap.get(hisWsItem.getId());
            if (workspaceInfoById != null) {
                workspaceInfoById.addHistoryWsItem(hisWsItem);
            }
            ScmWorkspaceInfo workspaceInfoByName = workspaceMapByName.get(hisWsItem.getName());
            if (workspaceInfoByName != null) {
                workspaceInfoByName.addHistoryWsItem(hisWsItem);
            }
        }
        finally {
            wLock.unlock();
        }
    }

    public void reloadSite(String siteName) throws ScmServerException {
        bizConf.reloadSite(siteName);
    }

    public void removeSite(String siteName) throws ScmServerException {
        bizConf.removeSite(siteName);
    }

    private ScmMetaService initRootSiteMeta(ScmSite info) throws ScmServerException {
        ScmSiteUrl metaUrl = info.getMetaUrl();
        logger.info("create meta service:type=" + metaUrl.getType() + ",siteId=" + info.getId()
                + ",user=" + metaUrl.getUser());
        return siteMgr.addMetaService(info.getId(), metaUrl);
    }

    private void initDataSourceService(ScmSite info) throws ScmServerException {
        ScmSiteUrl dataUrl = info.getDataUrl();
        logger.info("create data service:type=" + dataUrl.getType() + ",siteId=" + info.getId()
                + ",user=" + dataUrl.getUser());
        siteMgr.addDataService(info.getId(), dataUrl);
    }

    private void initSiteMgr() throws ScmServerException {
        int localSiteId = bizConf.getLocateSiteId();
        int rootSiteId = bizConf.getRootSiteId();
        Map<Integer, ScmSite> siteInfoMap = bizConf.getSiteMap();
        ScmSite rootSiteInfo = siteInfoMap.get(rootSiteId);
        if (null == rootSiteInfo) {
            throw new ScmInvalidArgumentException("root site is not exist:siteId=" + rootSiteId);
        }

        if (!rootSiteInfo.isRootSite()) {
            throw new ScmInvalidArgumentException(
                    "root site flag must be true:rootSiteId=" + rootSiteId);
        }

        initRootSiteMeta(rootSiteInfo);
        ScmSite info = siteInfoMap.get(localSiteId);
        if (null == info) {
            throw new ScmInvalidArgumentException("local site is not exist:siteId=" + localSiteId);
        }

        siteMgr.setOpFactory(info.getId(), info.getDataUrl().getType());
        initDataSourceService(info);
    }

    public ScmSite getSiteInfo(int siteId) {
        return bizConf.getSiteInfo(siteId);
    }

    public ScmSite getSiteInfo(String siteName) {
        return bizConf.getSiteInfo(siteName);
    }

    public Map<Integer, ScmSite> getAllSiteInfo() {
        return bizConf.getSiteMap();
    }

    public List<String> getServerConnectionList(int siteId) {
        List<String> connectionList = new ArrayList<>();
        List<ScmContentServerInfo> serverList = bizConf.getContentServers(siteId);
        for (ScmContentServerInfo csi : serverList) {
            connectionList.add(csi.getHostName() + ":" + csi.getPort());
        }

        return connectionList;
    }

    public List<ScmContentServerInfo> getContentServerList(int siteId) {
        return bizConf.getContentServers(siteId);
    }

    public List<ScmContentServerInfo> getContentServerList() {
        return bizConf.getContentServers();
    }

    public int getMainSite() {
        return bizConf.getRootSiteId();
    }

    public String getMainSiteName() {
        int id = getMainSite();
        return getSiteInfo(id).getName();
    }

    public int getLocalSite() {
        return bizConf.getLocateSiteId();
    }

    public ScmSite getLocalSiteInfo() {
        return getSiteInfo(getLocalSite());
    }

    public ScmMetaService getMetaService() throws ScmServerException {
        ScmMetaService sms = siteMgr.getMetaService();
        if (null == sms) {
            throw new ScmServerException(ScmError.SITE_NOT_EXIST, "meta site is not exist");
        }
        return sms;
    }

    public ScmService getDataService() throws ScmServerException {
        return siteMgr.getDataService();
    }

    public ScmWorkspaceInfo getWorkspaceInfo(int workspaceId) {
        ReadLock rLock = wsReadWriteLock.readLock();
        rLock.lock();
        try {
            return workspaceMap.get(workspaceId);
        }
        finally {
            rLock.unlock();
        }
    }

    public ScmWorkspaceInfo getWorkspaceInfoCheckLocalSite(int workspaceId)
            throws ScmServerException {
        int localSiteId = getLocalSite();
        ScmWorkspaceInfo wsInfo = getWorkspaceInfo(workspaceId);
        if (null == wsInfo) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "workspace is not exist:id=" + workspaceId);
        }

        boolean isServerInWorkspace = isSiteInWorkspace(wsInfo, localSiteId);

        if (!isServerInWorkspace) {
            throw new ScmServerException(ScmError.SERVER_NOT_IN_WORKSPACE, "my site[" + localSiteId
                    + "] is not in the workspace[" + wsInfo.getName() + "]");
        }

        return wsInfo;
    }

    private boolean isSiteInWorkspace(ScmWorkspaceInfo wsInfo, int siteId) {
        if (null != wsInfo.getSiteDataLocation(siteId)) {
            return true;
        }
        else {
            return false;
        }
    }

    public ScmWorkspaceInfo getWorkspaceInfo(String wsName) {
        ReadLock rLock = wsReadWriteLock.readLock();
        rLock.lock();
        try {
            return workspaceMapByName.get(wsName);
        }
        finally {
            rLock.unlock();
        }
    }

    public ScmWorkspaceInfo getWorkspaceInfoCheckExist(String wsName) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = getWorkspaceInfo(wsName);
        if (null == wsInfo) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "workspace is not exist:name=" + wsName);
        }

        return wsInfo;
    }

    public ScmWorkspaceInfo getWorkspaceInfoCheckLocalSite(String wsName)
            throws ScmServerException {
        int localSiteId = getLocalSite();
        ScmWorkspaceInfo wsInfo = getWorkspaceInfoCheckExist(wsName);

        boolean isServerInWorkspace = isSiteInWorkspace(wsInfo, localSiteId);

        if (!isServerInWorkspace) {
            throw new ScmServerException(ScmError.SERVER_NOT_IN_WORKSPACE,
                    "my site[" + getLocalSiteInfo().getName() + "] is not in the workspace["
                            + wsInfo.getName() + "]");
        }

        return wsInfo;
    }

    //
    // public List<ScmContentServerInfo> getOtherContentServers() {
    // return bizConf.getOtherContentServers();
    // }
    //
    // public List<ScmContentServerInfo> getOtherContentServers(int siteId) {
    // return bizConf.getOtherContentServers(siteId);
    // }
    //
    // public ScmContentServerInfo getServerInfo() {
    // return bizConf.getMyServer();
    // }

    public ScmContentServerInfo getServerInfo(int serverId) {
        return bizConf.getServerInfo(serverId);
    }

    public BSONObject getCurrentFileInfo(ScmWorkspaceInfo wsInfo, String fileID,
            boolean acceptDeleteMarker) throws ScmServerException {
        ScmMetaService ss = getMetaService();
        return ss.getCurrentFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileID,
                acceptDeleteMarker);
    }

    public void insertTransLog(String workspaceName, BSONObject transRecord)
            throws ScmServerException {
        ScmMetaService ss = getMetaService();
        ss.insertTransLog(workspaceName, transRecord);
    }

    public BSONObject getTransLog(String workspaceName, String transID) throws ScmServerException {
        ScmMetaService ss = getMetaService();
        return ss.getTransLog(workspaceName, transID);
    }

    public void deleteTransLog(String workspaceName, String transID) throws ScmServerException {
        ScmMetaService ss = getMetaService();
        ss.deleteTransLog(workspaceName, transID);
    }

    // public void deleteHistoryFile(String workspaceName, String fileID, int
    // majorVersion,
    // int minorVersion) throws ScmServerException {
    // ScmMetaService ss = getMetaService();
    // ss.deleteHistoryFile(workspaceName, fileID, majorVersion, minorVersion);
    // }

    public long getFileCount(ScmWorkspaceInfo wsInfo, BSONObject matcher)
            throws ScmServerException {
        ScmMetaService ss = getMetaService();

        return ss.getCurrentFileCount(wsInfo, matcher);
    }

    public boolean isInMainSite() {
        return this.bizConf.getRootSiteId() == bizConf.getLocateSiteId();
    }

    public void insertTask(BSONObject task) throws ScmServerException {
        ScmMetaService ss = getMetaService();
        ss.insertTask(task);
    }

    public long countTask(BSONObject matcher) throws ScmServerException {
        ScmMetaService ss = getMetaService();
        return ss.countTask(matcher);
    }

    public BSONObject getTaskInfo(BSONObject matcher) throws ScmServerException {
        ScmMetaService ss = getMetaService();
        return ss.getTask(matcher);
    }

    public ScmSiteMgr getSiteMgr() {
        return siteMgr;
    }

    public ScmBizConf getBizConf() {
        return bizConf;
    }

    public void reloadNode(String nodeName) throws ScmServerException {
        bizConf.reloadNode(nodeName);
    }

    public void removeNode(String nodeName) throws ScmServerException {
        bizConf.removeNode(nodeName);
    }
}
