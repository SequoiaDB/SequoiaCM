package com.sequoiacm.infrastructure.strategy.core;

import java.util.List;

import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;

public interface ConnectivityStrategy {
    
    StrategyType strategyType();

    void checkTransferSite(List<Integer> wsLocationSiteIds, int sourceSiteId, int targetSiteId) 
            throws StrategyException;
    
    void checkCleanSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException;
    
    void checkCacheSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException;
    
    List<Integer> getCleanTaskVerifySites(List<Integer> wsLocationSiteIds, List<Integer> fileLocationSites, 
            int localSiteId) throws StrategyException;
    
    SiteInfo getNearestSite(List<Integer> wsLocationSiteIds, List<Integer> fileLocationSites, int localSiteId) 
            throws StrategyException; 
    
    int getAsyncTransferTargetSite(List<Integer> wsLocationSiteIds, int localSiteId) 
            throws StrategyException;
}
