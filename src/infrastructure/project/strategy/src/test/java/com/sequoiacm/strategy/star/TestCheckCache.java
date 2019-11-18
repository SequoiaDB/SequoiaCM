package com.sequoiacm.strategy.star;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.infrastructure.strategy.core.ConnectivityStrategy;
import com.sequoiacm.infrastructure.strategy.core.StrategyFactory;
import com.sequoiacm.infrastructure.strategy.element.StrategyInfo;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.infrastructure.strategy.exception.StrategyException;
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;

public class TestCheckCache {

    private List<StrategyInfo> strategyList = new ArrayList<>();
    List<Integer> wsLocationSiteIds = new ArrayList<>();
    
    @BeforeClass
    public void setUp() {
        strategyList.add(new StrategyInfo(-1, -1, StrategyType.STAR.getName()));
        
        wsLocationSiteIds.add(1);
        wsLocationSiteIds.add(2);
        wsLocationSiteIds.add(3);
    }
    
    @Test
    public void testCacheNormal() {
        int localSiteId = 2;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkCacheSite(wsLocationSiteIds, localSiteId);
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testCacheInvalid() {
        int localSiteId = 1;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkCleanSite(wsLocationSiteIds, localSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyException.class, e.getClass(), e.getMessage());
        }
    }
    
    @Test
    public void testCacheInvalidArg() {
        int localSiteId = 1;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkCacheSite(null, localSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyInvalidArgumentException.class, e.getClass(), e.getMessage());
        }
    }
    
}
