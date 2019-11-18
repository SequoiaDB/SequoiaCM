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
import com.sequoiacm.infrastructure.strategy.exception.StrategyInvalidArgumentException;

public class TestGetCleanTaskVerifySites {

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
    public void testGetCleanTaskVerifySitesNormal() {
        int localSiteId = 2;
        int mainSiteId = 1;
        
        List<Integer> fileLocationSites = new ArrayList<>();
        fileLocationSites.add(1);
        fileLocationSites.add(2);
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            List<Integer> verifySites = strategy.getCleanTaskVerifySites(wsLocationSiteIds, fileLocationSites, localSiteId);
            Assert.assertEquals(verifySites.size(), 1);
            Assert.assertEquals(verifySites.get(0).intValue(), mainSiteId);
            
            fileLocationSites.remove(new Integer(mainSiteId));
            verifySites = strategy.getCleanTaskVerifySites(wsLocationSiteIds, fileLocationSites, localSiteId);
            Assert.assertEquals(verifySites.size(), 0);
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetCleanTaskVerifySitesInvalidArg() {
        int localSiteId = 2;
        int mainSiteId = 1;
        
        try {
            ConnectivityStrategy strategy = StrategyFactory.createStrategy(strategyList, mainSiteId);
            strategy.getCleanTaskVerifySites(wsLocationSiteIds, null, localSiteId);
        }
        catch (Exception e) {
            Assert.assertEquals(StrategyInvalidArgumentException.class, e.getClass(), e.getMessage());
        }
    }
}
