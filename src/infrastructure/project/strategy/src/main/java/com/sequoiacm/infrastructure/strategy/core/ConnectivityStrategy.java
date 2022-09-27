package com.sequoiacm.infrastructure.strategy.core;

import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;

import java.util.List;

public interface ConnectivityStrategy {
    
    StrategyType strategyType();

    void checkTransferSite(List<Integer> wsLocationSiteIds, int sourceSiteId, int targetSiteId) 
            throws StrategyException;

    void checkMoveFileSite(List<Integer> wsLocationSiteIds, int sourceSiteId, int targetSiteId)
            throws StrategyException;
    
    void checkCleanSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException;
    
    void checkCacheSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException;
    
    SiteInfo getNearestSite(List<Integer> wsLocationSiteIds, List<Integer> fileLocationSites, int localSiteId) 
            throws StrategyException; 
    
    int getAsyncTransferTargetSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException;
}
