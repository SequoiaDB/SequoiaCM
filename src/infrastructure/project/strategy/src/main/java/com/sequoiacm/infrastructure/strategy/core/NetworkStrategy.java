package com.sequoiacm.infrastructure.strategy.core;

import com.sequoiacm.infrastructure.strategy.common.StrategyDefine;
import com.sequoiacm.infrastructure.strategy.common.StrategyTools;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;

import java.util.ArrayList;
import java.util.List;

public class NetworkStrategy implements ConnectivityStrategy {

    private List<StrategyInfo> strategies;
    
    public NetworkStrategy(List<StrategyInfo> strategies) throws StrategyException {
        if (null == strategies) {
            throw new StrategyException("Strategy info list cannot be null");
        }
        this.strategies = strategies;
    }
    
    @Override
    public StrategyType strategyType() {
        return StrategyType.NETWORK;
    }
    
    @Override
    public void checkTransferSite(List<Integer> wsLocationSiteIds, int sourceSiteId, int targetSiteId) 
            throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, sourceSiteId);
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, targetSiteId);
        /*
        if (StrategyTools.isLastSiteInList(wsLocationSiteIds, sourceSiteId)) {
            throw new StrategyException("Under the network strategy,"
                    + "transfer task's source site cannot be last site in the workspace");
        }
        */
        if (sourceSiteId == targetSiteId) {
            throw new StrategyException("The source site and target site cannot be the same"
                    + ":sourceSite=" + sourceSiteId + ",targetSite=" + targetSiteId);
        }
        if (!StrategyTools.isConnected(this.strategies, sourceSiteId, targetSiteId)) {
            throw new StrategyException("The source site cannot connect to the target site:sourceSite=" 
                    + sourceSiteId + ",targetSite=" + targetSiteId);
        }
    }

    @Override
    public void checkCleanSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        /*
        if (StrategyTools.isLastSiteInList(wsLocationSiteIds, localSiteId)) {
            throw new StrategyException("Under the network strategy,"
                    + "the site that performing the clean task cannot be the last site in the workspace"
                    + ":siteId=" + localSiteId);
        }
        */
    }
    
    @Override
    public void checkCacheSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException {
        // unlimited
    }

    @Override
    public SiteInfo getNearestSite(List<Integer> wsLocationSiteIds, List<Integer> fileLocationSites, 
            int localSiteId) throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        checkFileLocationSiteNotNull(fileLocationSites);
        Integer id = StrategyTools.getSiteIdInOrder(wsLocationSiteIds, fileLocationSites, localSiteId);
        if (null == id) {
            throw new StrategyException("Not found optional site:wsLocationSite=" + wsLocationSiteIds 
                    + ",fileLocationSite=" + fileLocationSites);
        }
        SiteInfo siteInfo = new SiteInfo();
        siteInfo.setId(id);
        siteInfo.setFlag(StrategyDefine.SiteType.FLAG_NORMAL_SITE);
        return siteInfo;
    }
    
    @Override
    public int getAsyncTransferTargetSite(List<Integer> wsLocationSiteIds, int localSiteId)
            throws StrategyException {
        checkSiteInWorkspaceOrNot(wsLocationSiteIds, localSiteId);
        if (StrategyTools.isLastSiteInList(wsLocationSiteIds, localSiteId)) {
            throw new StrategyException("Under the network strategy,"
                    + "if do not specify a target site, transfer single file's source site cannot be the last site in the workspace:siteId="
                    + localSiteId);
        }
        List<Integer> failSiteList = new ArrayList<>();
        int sourceIdx = wsLocationSiteIds.indexOf(localSiteId);
        for (int i=sourceIdx+1, len=wsLocationSiteIds.size(); i < len; i++) {
            int targetSiteId = wsLocationSiteIds.get(i);
            if (StrategyTools.isConnected(this.strategies, localSiteId, targetSiteId)) {
                return targetSiteId;
            }
            failSiteList.add(targetSiteId);
        }
        throw new StrategyException("Available target site=" + failSiteList + ",but cannot be connected");
    }
    
    private void checkSiteInWorkspaceOrNot(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyInvalidArgumentException {
        if (null == wsLocationSiteIds || wsLocationSiteIds.size() == 0) {
            throw new StrategyInvalidArgumentException("Workspace location site list cannot be empty");
        }
        if (!wsLocationSiteIds.contains(localSiteId)) {
            throw new StrategyInvalidArgumentException(
                    "site[" + localSiteId + "] is not in the workspace location site list" + wsLocationSiteIds);
        }
    }
    
    private void checkFileLocationSiteNotNull(List<Integer> fileLocationSiteIds) 
            throws StrategyInvalidArgumentException {
        if (null == fileLocationSiteIds) {
            throw new StrategyInvalidArgumentException("File location site list cannot be null");
        }
    }
}
