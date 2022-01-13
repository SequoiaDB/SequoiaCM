package com.sequoiacm.contentserver.site;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceMgr;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaWorkspaceAccessor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ScmBizConf {
    private static final Logger logger = LoggerFactory.getLogger(ScmBizConf.class);

    private int rootSiteId = -1;

    private ScmSite mySite;
    private Map<Integer, ScmSite> siteInfoMap = new HashMap<>();
    private Map<String, ScmSite> siteInfoMapByName = new HashMap<>();
    private ReentrantReadWriteLock siteReadWriteLock = new ReentrantReadWriteLock();

    private Map<Integer, Map<String, ScmContentServerInfo>> serverMapBySiteId = new HashMap<>();
    private Map<String, ScmContentServerInfo> serverMapByName = new HashMap<>();
    private Map<Integer, ScmContentServerInfo> serverMap = new HashMap<>();
    private ReentrantReadWriteLock nodeReadWriterLock = new ReentrantReadWriteLock();

    private List<BSONObject> workspaceInfoList = Collections
            .synchronizedList(new ArrayList<BSONObject>());
    private String mySiteName;

    public void init(String mySiteName) throws ScmServerException {
        this.mySiteName = mySiteName;
        ContentModuleMetaSource metasource = null;
        try {
            String user = PropertiesUtils.getRootSiteUser();
            AuthInfo auth = ScmFilePasswordParser.parserFile(PropertiesUtils.getRootSitePassword());

            String mainSiteUrl = PropertiesUtils.getRootSiteUrl();

            logger.info("connecting to site:url=" + mainSiteUrl);
            String[] urlArray = mainSiteUrl.split(",");
            List<String> urlList = new ArrayList<>();
            Collections.addAll(urlList, urlArray);
            metasource = ScmMetaSourceMgr.getInstance().createInitPhaseMetaSource(urlList, user,
                    auth.getPassword());
            initSystemInfo(metasource);
            checkIsUrlInRootSite(mainSiteUrl);
        }
        catch (ScmServerException e) {
            clear();
            throw e;
        }
        finally {
            if (null != metasource) {
                metasource.close();
            }
        }
    }

    private void checkIsUrlInRootSite(String url) throws ScmServerException {
        String[] urlArray = url.split(",");

        ScmSite site = getSiteInfo(rootSiteId);
        if (null == site) {
            throw new ScmInvalidArgumentException("root site is missing:rootSiteId=" + rootSiteId);
        }

        List<String> urls = site.getMetaUrl().getUrls();
        Set<String> urlSet = new HashSet<>();
        urlSet.addAll(urls);
        for (int i = 0; i < urlArray.length; i++) {
            if (ScmSystemUtils.checkUrlExist(urlSet, urlArray[i])) {
                return;
            }
        }

        throw new ScmSystemException(PropertiesDefine.PROPERTY_ROOTSITE_URL + " is not exist in "
                + ServiceDefine.CsName.CS_SCMSYSTEM + "." + ServiceDefine.SystemClName.CL_SITE
                + ",url=" + url);
    }

    private void initSystemInfo(ContentModuleMetaSource metasource) throws ScmServerException {
        List<ScmSite> siteList = null;
        List<ScmContentServerMapping> serverList = null;

        logger.info("start to reading site info");
        siteList = ScmMetaSourceHelper.getSiteList(metasource);
        if (siteList.size() == 0) {
            throw new ScmServerException(ScmError.OUT_OF_BOUND, "site is not exist");
        }

        logger.info("start to reading server info");
        serverList = ScmMetaSourceHelper.getServerList(metasource);
        if (serverList.size() == 0) {
            throw new ScmServerException(ScmError.OUT_OF_BOUND, "content server is not exist");
        }

        logger.info("start to reading workspace info");
        workspaceInfoList = ScmMetaSourceHelper.getWorkspaceList(metasource);

        checkAndSetMainSite(siteList);
        loadSiteInfo(siteList);
        loadServerInfo(serverList);
    }

    private void loadServerInfo(List<ScmContentServerMapping> serverList)
            throws ScmServerException {
        logger.info("start to parse server info");
        for (int i = 0; i < serverList.size(); i++) {
            ScmContentServerMapping scsm = serverList.get(i);
            ScmSite siteInfo = getSiteInfo(scsm.getSite_id());
            if (null != siteInfo) {
                ScmContentServerInfo nodeInfo = new ScmContentServerInfo(scsm.getId(),
                        scsm.getName(), siteInfo, scsm.getHost_name(), scsm.getPort(),
                        scsm.getType());
                reloadNode(nodeInfo);
            }
        }
        //
        // if (null == myServer) {
        // throw new ScmServerException(ScmError.METASOURCE_ERROR,
        // "this server is not exist in SCM:host=" + ScmSystemUtils.getHostName()
        // + ",port=" + PropertiesUtils.getServerPort());
        // }
    }

    private void loadSiteInfo(List<ScmSite> siteList) throws ScmServerException {
        logger.info("start to parse site info");
        for (int i = 0; i < siteList.size(); i++) {
            ScmSite oneSite = siteList.get(i);
            reloadSite(oneSite);
        }
        if (mySite == null) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "content module did not bind any site: bindingSite=" + mySiteName
                            + ", siteList=" + siteList);
        }
    }

    private void checkAndSetMainSite(List<ScmSite> siteList) throws ScmServerException {
        for (int i = 0; i < siteList.size(); i++) {
            ScmSite oneSite = siteList.get(i);
            if (oneSite.isRootSite()) {
                if (rootSiteId != -1) {
                    throw new ScmInvalidArgumentException("there exist two" + " rootSite:site1="
                            + rootSiteId + ",site2=" + oneSite.getId());
                }
                rootSiteId = oneSite.getId();
            }
        }

        if (rootSiteId == -1) {
            throw new ScmInvalidArgumentException("root site must exist in table"
                    + ServiceDefine.CsName.CS_SCMSYSTEM + "." + ServiceDefine.SystemClName.CL_SITE);
        }
    }

    public void clear() {
        WriteLock wSiteLock = siteReadWriteLock.writeLock();
        wSiteLock.lock();
        try {
            siteInfoMap.clear();
            siteInfoMapByName.clear();
        }
        finally {
            wSiteLock.unlock();
        }

        WriteLock wNodeLock = nodeReadWriterLock.writeLock();
        wNodeLock.lock();
        try {
            serverMap.clear();
            serverMapByName.clear();
            serverMapBySiteId.clear();
        }
        finally {
            wNodeLock.unlock();
        }

        workspaceInfoList.clear();
    }

    public ScmSite getSiteInfo(int siteId) {
        ReadLock rLock = siteReadWriteLock.readLock();
        rLock.lock();
        try {
            return siteInfoMap.get(siteId);
        }
        finally {
            rLock.unlock();
        }
    }
    //
    // public ScmContentServerInfo getMyServer() {
    // return myServer;
    // }

    public int getRootSiteId() {
        return rootSiteId;
    }

    public int getLocateSiteId() {
        return mySite.getId();
    }

    public Map<Integer, ScmSite> getSiteMap() {
        ReadLock rLock = siteReadWriteLock.readLock();
        rLock.lock();
        try {
            return new HashMap<>(siteInfoMap);
        }
        finally {
            rLock.unlock();
        }
    }

    public ScmSite getSiteInfo(String siteName) {
        ReadLock rLock = siteReadWriteLock.readLock();
        rLock.lock();
        try {
            return siteInfoMapByName.get(siteName);
        }
        finally {
            rLock.unlock();
        }
    }

    public List<ScmContentServerInfo> getContentServers() {
        ReadLock rLock = nodeReadWriterLock.readLock();
        rLock.lock();
        try {
            return new ArrayList<>(serverMapByName.values());
        }
        finally {
            rLock.unlock();
        }
    }

    public List<ScmContentServerInfo> getContentServers(int siteId) {
        ReadLock rLock = nodeReadWriterLock.readLock();
        rLock.lock();
        try {
            Map<String, ScmContentServerInfo> tmpServerMap = serverMapBySiteId.get(siteId);
            List<ScmContentServerInfo> contentServers = new ArrayList<>();
            if (tmpServerMap != null) {
                contentServers.addAll(tmpServerMap.values());
            }
            return contentServers;
        }
        finally {
            rLock.unlock();
        }
    }
    //
    // public List<ScmContentServerInfo> getOtherContentServers() {
    // ReadLock rLock = nodeReadWriterLock.readLock();
    // rLock.lock();
    // try {
    // Map<String, ScmContentServerInfo> tmpServerMapByName = new HashMap<>();
    // tmpServerMapByName.putAll(serverMapByName);
    // tmpServerMapByName.remove(myServer.getName());
    // return new ArrayList<>(tmpServerMapByName.values());
    // }
    // finally {
    // rLock.unlock();
    // }
    // }
    //
    // public List<ScmContentServerInfo> getOtherContentServers(int siteId) {
    // ReadLock rLock = nodeReadWriterLock.readLock();
    // rLock.lock();
    // try {
    // Map<String, ScmContentServerInfo> ServerMapInSite =
    // serverMapBySiteId.get(siteId);
    // List<ScmContentServerInfo> otherContentServers = new ArrayList<>();
    // if (ServerMapInSite != null) {
    // Map<String, ScmContentServerInfo> tmpServerMap = new HashMap<>();
    // tmpServerMap.putAll(ServerMapInSite);
    // tmpServerMap.remove(myServer.getName());
    // otherContentServers.addAll(tmpServerMap.values());
    // }
    // return otherContentServers;
    // }
    // finally {
    // rLock.unlock();
    // }
    // }

    public ScmContentServerInfo getServerInfo(int serverId) {
        ReadLock rLock = nodeReadWriterLock.readLock();
        rLock.lock();
        try {
            return serverMap.get(serverId);
        }
        finally {
            rLock.unlock();
        }
    }

    public List<BSONObject> getWorkspaceList() {
        return this.workspaceInfoList;
    }

    public void removeWorkspace(String wsName) {
        for (int i = 0; i < workspaceInfoList.size(); i++) {
            if (workspaceInfoList.get(i).get(FieldName.FIELD_CLWORKSPACE_NAME).equals(wsName)) {
                workspaceInfoList.remove(i);
                return;
            }
        }
    }

    public BSONObject reloadWorkspace(String wsName) throws ScmServerException {
        MetaWorkspaceAccessor assersor = ScmContentServer.getInstance().getMetaService()
                .getMetaSource().getWorkspaceAccessor();
        BSONObject wsObj = ScmMetaSourceHelper.queryOne(assersor,
                new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName));

        // replace old workspace bson
        for (int i = 0; i < workspaceInfoList.size(); i++) {
            if (workspaceInfoList.get(i).get(FieldName.FIELD_CLWORKSPACE_NAME).equals(wsName)) {
                workspaceInfoList.remove(i);
                if (wsObj != null) {
                    workspaceInfoList.add(i, wsObj);
                }
                return wsObj;
            }
        }

        // new workspace not exist return null
        if (wsObj == null) {
            return null;
        }

        // insert new workspace to wsList (order by id)
        int newWsId = (int) wsObj.get(FieldName.FIELD_CLWORKSPACE_ID);
        for (int i = 0; i < workspaceInfoList.size(); i++) {
            int id = (int) workspaceInfoList.get(i).get(FieldName.FIELD_CLWORKSPACE_ID);
            if (newWsId < id) {
                workspaceInfoList.add(i, wsObj);
                return wsObj;
            }
        }
        workspaceInfoList.add(wsObj);
        return wsObj;
    }

    private BSONObject queryOneSite(String siteName) throws ScmServerException {
        MetaAccessor assersor = ScmContentServer.getInstance().getMetaService().getMetaSource()
                .getSiteAccessor();
        BSONObject siteBson = ScmMetaSourceHelper.queryOne(assersor,
                new BasicBSONObject(FieldName.FIELD_CLSITE_NAME, siteName));
        return siteBson;
    }

    private void reloadSite(ScmSite site) throws ScmServerException {
        WriteLock wLock = siteReadWriteLock.writeLock();
        wLock.lock();
        try {
            if (rootSiteId == -1 && site.isRootSite()) {
                rootSiteId = site.getId();
                // create root site, init strategy mgr
                List<BSONObject> strategyList = ScmContentServer.getInstance().getMetaService()
                        .getAllStrategyInfo();
                ScmStrategyMgr.getInstance().init(strategyList, rootSiteId);
            }
            siteInfoMapByName.put(site.getName(), site);
            siteInfoMap.put(site.getId(), site);
            logger.info("reload site cache:siteName={}, isRootSite={}", site.getName(),
                    site.isRootSite());
        }
        finally {
            wLock.unlock();
        }
        if (site.getName().equals(mySiteName)) {
            mySite = site;
        }
    }

    public ScmSite getMySite() {
        return mySite;
    }

    public void removeSite(String siteName) {
        WriteLock wLock = siteReadWriteLock.writeLock();
        wLock.lock();
        try {
            ScmSite site = siteInfoMapByName.remove(siteName);
            if (site != null) {
                siteInfoMap.remove(site.getId());
                removeNodesBySiteId(site.getId());
                logger.info("remove site cache:siteName={}", siteName);
            }
        }
        finally {
            wLock.unlock();
        }
    }

    private void removeNodesBySiteId(int siteId) {
        Lock wlock = nodeReadWriterLock.writeLock();
        wlock.lock();
        try {
            Map<String, ScmContentServerInfo> serverNodeMap = serverMapBySiteId.remove(siteId);
            if (serverNodeMap != null) {
                Set<String> nodeNameSet = serverNodeMap.keySet();
                for (String nodeName : nodeNameSet) {
                    ScmContentServerInfo nodeInfo = serverMapByName.remove(nodeName);
                    serverMap.remove(nodeInfo.getId());
                    logger.info("remove node cache in site: nodeName={}, siteId={}", nodeName,
                            siteId);
                }
            }
        }
        finally {
            wlock.unlock();
        }
    }

    public void reloadSite(String siteName) throws ScmServerException {
        BSONObject siteBson = queryOneSite(siteName);
        if (siteBson != null) {
            ScmSite site = new ScmSite(siteBson);
            reloadSite(site);
        }
        else {
            removeSite(siteName);
        }

    }

    private void reloadNode(ScmContentServerInfo nodeInfo) {
        Lock wlock = nodeReadWriterLock.writeLock();
        wlock.lock();
        try {
            String nodeName = nodeInfo.getName();
            serverMapByName.put(nodeName, nodeInfo);
            serverMap.put(nodeInfo.getId(), nodeInfo);

            // // reload loacl server node
            // if (ScmSystemUtils.isLocalHost(nodeInfo.getHostName())
            // && nodeInfo.getPort() == PropertiesUtils.getServerPort()) {
            // myServer = nodeInfo;
            // }

            // reload server node in site
            int siteId = nodeInfo.getSite().getId();
            Map<String, ScmContentServerInfo> serverNodeMap = serverMapBySiteId.get(siteId);
            if (serverNodeMap == null) {
                serverNodeMap = new HashMap<>();
                serverNodeMap.put(nodeName, nodeInfo);
                serverMapBySiteId.put(siteId, serverNodeMap);
            }
            else {
                serverNodeMap.put(nodeName, nodeInfo);
            }

            logger.info("reload node cache:nodeName={}", nodeInfo.getName());
        }
        finally {
            wlock.unlock();
        }
    }

    public void removeNode(String nodeName) {
        Lock wlock = nodeReadWriterLock.writeLock();
        wlock.lock();
        try {
            ScmContentServerInfo nodeInfo = serverMapByName.remove(nodeName);

            if (nodeInfo != null) {
                serverMap.remove(nodeInfo.getId());

                // remove server node in site
                int siteId = nodeInfo.getSite().getId();
                Map<String, ScmContentServerInfo> serverNodeMap = serverMapBySiteId.get(siteId);
                if (serverNodeMap != null) {
                    serverNodeMap.remove(nodeName);
                    if (serverNodeMap.size() == 0) {
                        serverMapBySiteId.remove(siteId);
                    }
                }
                logger.info("remove node cache:nodeName={}", nodeName);
            }

        }
        finally {
            wlock.unlock();
        }
    }

    public void reloadNode(String nodeName) throws ScmServerException {
        BSONObject nodeBson = queryOneSreverNode(nodeName);
        if (nodeBson != null) {
            ScmContentServerMapping nodeMapping = new ScmContentServerMapping(nodeBson);
            ScmSite siteInfo = getSiteInfo(nodeMapping.getSite_id());
            if (siteInfo != null) {
                ScmContentServerInfo nodeInfo = new ScmContentServerInfo(nodeMapping.getId(),
                        nodeMapping.getName(), siteInfo, nodeMapping.getHost_name(),
                        nodeMapping.getPort(), nodeMapping.getType());
                reloadNode(nodeInfo);
            }
        }
        else {
            removeNode(nodeName);
        }
    }

    private BSONObject queryOneSreverNode(String nodeName) throws ScmServerException {
        MetaAccessor assersor = ScmContentServer.getInstance().getMetaService().getMetaSource()
                .getServerAccessor();
        BSONObject nodeBson = ScmMetaSourceHelper.queryOne(assersor,
                new BasicBSONObject(FieldName.FIELD_CLCONTENTSERVER_NAME, nodeName));
        return nodeBson;
    }
}
