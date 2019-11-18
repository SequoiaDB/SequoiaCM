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

public class TestCheckTransfer {

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
    public void testTransferNormal() {
        int sourceSiteId = 2;
        int targetSiteId = 1;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkTransferSite(wsLocationSiteIds, sourceSiteId, targetSiteId);
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testTransferSourceInvalid() {
        int sourceSiteId = 1;
        int targetSiteId = 2;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkTransferSite(wsLocationSiteIds, sourceSiteId, targetSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyException.class, e.getClass(), e.getMessage());
        }
    }
    
    @Test
    public void testTransferTargetInvalid() {
        int sourceSiteId = 2;
        int targetSiteId = 3;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkTransferSite(wsLocationSiteIds, sourceSiteId, targetSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyException.class, e.getClass(), e.getMessage());
        }
    }
    
    @Test
    public void testTransferTargetInvalidArg() {
        int sourceSiteId = 2;
        int targetSiteId = 1;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.checkTransferSite(null, sourceSiteId, targetSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyInvalidArgumentException.class, e.getClass(), e.getMessage());
        }
    }
}
